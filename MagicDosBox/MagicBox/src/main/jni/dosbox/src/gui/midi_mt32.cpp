#include <SDL_thread.h>
#include <SDL_endian.h>
#include "control.h"

#ifndef DOSBOX_MIDI_H
#include "midi.h"
#endif

#include "midi_mt32.h"

static const Bitu MILLIS_PER_SECOND = 1000;

MidiHandler_mt32 &MidiHandler_mt32::GetInstance() {
	static MidiHandler_mt32 midiHandler_mt32;
	return midiHandler_mt32;
}

const char *MidiHandler_mt32::GetName(void) {
	return "mt32";
}

bool MidiHandler_mt32::Open(const char *conf) {
	Section_prop *section = static_cast<Section_prop *>(control->GetSection("midi"));

	const char* s_mpu = section->Get_string("mpu401");
	if(strcasecmp(s_mpu,"none") == 0) return false;
	if(strcasecmp(s_mpu,"off") == 0) return false;
	if(strcasecmp(s_mpu,"false") == 0) return false;

	const char *romDir = section->Get_string("mt32.romdir");
	if (romDir == NULL) romDir = "./"; // Paranoid NULL-check, should never happen
	size_t romDirLen = strlen(romDir);
	bool addPathSeparator = false;
	if (romDirLen < 1) {
		romDir = "./";
	} else if (4080 < romDirLen) {
		LOG_MSG("MT32: mt32.romdir is too long, using the current dir.");
		romDir = "./";
	} else {
		char lastChar = romDir[strlen(romDir) - 1];
		addPathSeparator = lastChar != '/' && lastChar != '\\';
	}

	char pathName[4096];
	MT32Emu::FileStream controlROMFile;
	MT32Emu::FileStream pcmROMFile;
///storage/emulated/0/MagicBox/MT32ROM/
	makeROMPathName(pathName, romDir, "CM32L_CONTROL.ROM", addPathSeparator);
	if (!controlROMFile.open(pathName)) {
		makeROMPathName(pathName, romDir, "MT32_CONTROL.ROM", addPathSeparator);
		if (!controlROMFile.open(pathName)) {
			//LOG_MSG("MT32: Control ROM file not found");
			return false;
		}
	}
	makeROMPathName(pathName, romDir, "CM32L_PCM.ROM", addPathSeparator);
	if (!pcmROMFile.open(pathName)) {
		makeROMPathName(pathName, romDir, "MT32_PCM.ROM", addPathSeparator);
		if (!pcmROMFile.open(pathName)) {
			//LOG_MSG("MT32: PCM ROM file not found");
			return false;
		}
	}

	const MT32Emu::ROMImage *controlROMImage = MT32Emu::ROMImage::makeROMImage(&controlROMFile);
	const MT32Emu::ROMImage *pcmROMImage = MT32Emu::ROMImage::makeROMImage(&pcmROMFile);

	MT32Emu::AnalogOutputMode analogOutputMode = (MT32Emu::AnalogOutputMode)section->Get_int("mt32.analog");

	synth = new MT32Emu::Synth(&reportHandler);
	if (!synth->open(*controlROMImage, *pcmROMImage, section->Get_int("mt32.partials"), analogOutputMode)) {
		delete synth;
		synth = NULL;
		//LOG_MSG("MT32: Error initialising emulation");
		return false;
	}
	MT32Emu::ROMImage::freeROMImage(controlROMImage);
	MT32Emu::ROMImage::freeROMImage(pcmROMImage);

	if (strcmp(section->Get_string("mt32.reverb.mode"), "auto") != 0) {
		Bit8u reverbsysex[] = {0x10, 0x00, 0x01, 0x00, 0x05, 0x03};
		reverbsysex[3] = (Bit8u)atoi(section->Get_string("mt32.reverb.mode"));
		reverbsysex[4] = (Bit8u)section->Get_int("mt32.reverb.time");
		reverbsysex[5] = (Bit8u)section->Get_int("mt32.reverb.level");
		synth->writeSysex(16, reverbsysex, 6);
		synth->setReverbOverridden(true);
	}

	synth->setDACInputMode((MT32Emu::DACInputMode)section->Get_int("mt32.dac"));

	synth->setReversedStereoEnabled(section->Get_bool("mt32.reverse.stereo"));
	noise = section->Get_bool("mt32.verbose");
	renderInThread = section->Get_bool("mt32.thread");

	if (noise) LOG_MSG("MT32: Set maximum number of partials %d", synth->getPartialCount());
	if (noise) LOG_MSG("MT32: Adding mixer channel at sample rate %d", synth->getStereoOutputSampleRate());
	chan = MIXER_AddChannel(mixerCallBack, synth->getStereoOutputSampleRate(), "MT32");
	if (renderInThread) {
		stopProcessing = false;
		playPos = 0;
		sampleRateRatio = MT32Emu::SAMPLE_RATE / (double)synth->getStereoOutputSampleRate();
		int chunkSize = section->Get_int("mt32.chunk");
		minimumRenderFrames = (chunkSize * synth->getStereoOutputSampleRate()) / MILLIS_PER_SECOND;
		int latency = section->Get_int("mt32.prebuffer");
		if (latency <= chunkSize) {
			latency = 2 * chunkSize;
			LOG_MSG("MT32: chunk length must be less than prebuffer length, prebuffer length reset to %i ms.", latency);
		}
		framesPerAudioBuffer = (latency * synth->getStereoOutputSampleRate()) / MILLIS_PER_SECOND;
		audioBufferSize = framesPerAudioBuffer << 1;
		audioBuffer = new Bit16s[audioBufferSize];
		synth->render(audioBuffer, framesPerAudioBuffer - 1);
		renderPos = (framesPerAudioBuffer - 1) << 1;
		playedBuffers = 1;
		lock = SDL_CreateMutex();
		framesInBufferChanged = SDL_CreateCond();
		thread = SDL_CreateThread(processingThread, NULL);
	}
	chan->Enable(true);

	open = true;
	return true;
}

void MidiHandler_mt32::Close(void) {
	if (!open) return;
	chan->Enable(false);
	if (renderInThread) {
		stopProcessing = true;
		SDL_LockMutex(lock);
		SDL_CondSignal(framesInBufferChanged);
		SDL_UnlockMutex(lock);
		SDL_WaitThread(thread, NULL);
		thread = NULL;
		SDL_DestroyMutex(lock);
		lock = NULL;
		SDL_DestroyCond(framesInBufferChanged);
		framesInBufferChanged = NULL;
		delete[] audioBuffer;
		audioBuffer = NULL;
	}
	MIXER_DelChannel(chan);
	chan = NULL;
	synth->close();
	delete synth;
	synth = NULL;
	open = false;
}

void MidiHandler_mt32::PlayMsg(Bit8u *msg) {
	if (renderInThread) {
		synth->playMsg(SDL_SwapLE32(*(Bit32u *)msg), getMidiEventTimestamp());
	} else {
		synth->playMsg(SDL_SwapLE32(*(Bit32u *)msg));
	}
}

void MidiHandler_mt32::PlaySysex(Bit8u *sysex, Bitu len) {
	if (renderInThread) {
		synth->playSysex(sysex, len, getMidiEventTimestamp());
	} else {
		synth->playSysex(sysex, len);
	}
}

void MidiHandler_mt32::mixerCallBack(Bitu len) {
	MidiHandler_mt32::GetInstance().handleMixerCallBack(len);
}

int MidiHandler_mt32::processingThread(void *) {
	MidiHandler_mt32::GetInstance().renderingLoop();
	return 0;
}

void MidiHandler_mt32::makeROMPathName(char pathName[], const char romDir[], const char fileName[], bool addPathSeparator) {
	strcpy(pathName, romDir);
	if (addPathSeparator) {
		strcat(pathName, "/");
	}
	strcat(pathName, fileName);
}

MidiHandler_mt32::MidiHandler_mt32() : open(false), chan(NULL), synth(NULL), thread(NULL) {
}

MidiHandler_mt32::~MidiHandler_mt32() {
	Close();
}

void MidiHandler_mt32::handleMixerCallBack(Bitu len) {
	if (renderInThread) {
		while (renderPos == playPos) {
			SDL_LockMutex(lock);
			SDL_CondWait(framesInBufferChanged, lock);
			SDL_UnlockMutex(lock);
			if (stopProcessing) return;
		}
		Bitu renderPosSnap = renderPos;
		Bitu playPosSnap = playPos;
		Bitu samplesReady = (renderPosSnap < playPosSnap) ? audioBufferSize - playPosSnap : renderPosSnap - playPosSnap;
		if (len > (samplesReady >> 1)) {
			len = samplesReady >> 1;
		}
		chan->AddSamples_s16(len, audioBuffer + playPosSnap);
		playPosSnap += (len << 1);
		while (audioBufferSize <= playPosSnap) {
			playPosSnap -= audioBufferSize;
			playedBuffers++;
		}
		playPos = playPosSnap;
		renderPosSnap = renderPos;
		const Bitu samplesFree = (renderPosSnap < playPosSnap) ? playPosSnap - renderPosSnap : audioBufferSize + playPosSnap - renderPosSnap;
		if (minimumRenderFrames <= (samplesFree >> 1)) {
			SDL_LockMutex(lock);
			SDL_CondSignal(framesInBufferChanged);
			SDL_UnlockMutex(lock);
		}
	} else {
		synth->render((Bit16s *)MixTemp, len);
		chan->AddSamples_s16(len, (Bit16s *)MixTemp);
	}
}

void MidiHandler_mt32::renderingLoop() {
	while (!stopProcessing) {
		const Bitu renderPosSnap = renderPos;
		const Bitu playPosSnap = playPos;
		Bitu samplesToRender;
		if (renderPosSnap < playPosSnap) {
			samplesToRender = playPosSnap - renderPosSnap - 2;
		} else {
			samplesToRender = audioBufferSize - renderPosSnap;
			if (playPosSnap == 0) samplesToRender -= 2;
		}
		Bitu framesToRender = samplesToRender >> 1;
		if ((framesToRender == 0) || ((framesToRender < minimumRenderFrames) && (renderPosSnap < playPosSnap))) {
			SDL_LockMutex(lock);
			SDL_CondWait(framesInBufferChanged, lock);
			SDL_UnlockMutex(lock);
		} else {
			synth->render(audioBuffer + renderPosSnap, framesToRender);
			renderPos = (renderPosSnap + samplesToRender) % audioBufferSize;
			if (renderPosSnap == playPos) {
				SDL_LockMutex(lock);
				SDL_CondSignal(framesInBufferChanged);
				SDL_UnlockMutex(lock);
			}
		}
	}
}

void MidiHandler_mt32::MT32ReportHandler::onErrorControlROM() {
//	CatLog::log("MT32: Couldn't open Control ROM file");
}

void MidiHandler_mt32::MT32ReportHandler::onErrorPCMROM() {
//	CatLog::log("MT32: Couldn't open PCM ROM file");
}

void MidiHandler_mt32::MT32ReportHandler::showLCDMessage(const char *message) {
//	CatLog::log("MT32: LCD-Message: %s", message);
}

void MidiHandler_mt32::MT32ReportHandler::printDebug(const char *fmt, va_list list) {
	MidiHandler_mt32 &midiHandler_mt32 = MidiHandler_mt32::GetInstance();
	if (midiHandler_mt32.noise) {
		char s[1024];
		strcpy(s, "MT32: ");
		vsnprintf(s + 6, 1017, fmt, list);
		//LOG_MSG(s);
	}
}
