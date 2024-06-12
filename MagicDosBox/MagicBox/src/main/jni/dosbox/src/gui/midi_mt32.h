#ifndef DOSBOX_MIDI_MT32_H
#define DOSBOX_MIDI_MT32_H

#include "mixer.h"
#include "../../mt32emu/src/mt32emu.h"

struct SDL_Thread;

class MidiHandler_mt32 : public MidiHandler {
public:
	static MidiHandler_mt32 &GetInstance(void);

	const char *GetName(void);
	bool Open(const char *conf);
	void Close(void);
	void PlayMsg(Bit8u *msg);
	void PlaySysex(Bit8u *sysex, Bitu len);

private:
	MixerChannel *chan;
	MT32Emu::Synth *synth;
	SDL_Thread *thread;
	SDL_mutex *lock;
	SDL_cond *framesInBufferChanged;
	Bit16s *audioBuffer;
	Bitu audioBufferSize;
	Bitu framesPerAudioBuffer;
	Bitu minimumRenderFrames;
	double sampleRateRatio;
	volatile Bitu renderPos, playPos, playedBuffers;
	volatile bool stopProcessing;
	bool open, noise, renderInThread;

	class MT32ReportHandler : public MT32Emu::ReportHandler {
	protected:
		virtual void onErrorControlROM();
		virtual void onErrorPCMROM();
		virtual void showLCDMessage(const char *message);
		virtual void printDebug(const char *fmt, va_list list);
	} reportHandler;

	static void mixerCallBack(Bitu len);
	static int processingThread(void *);
	static void makeROMPathName(char pathName[], const char romDir[], const char fileName[], bool addPathSeparator);

	MidiHandler_mt32();
	~MidiHandler_mt32();

	Bit32u inline getMidiEventTimestamp() {
		return Bit32u((playedBuffers * framesPerAudioBuffer + (playPos >> 1)) * sampleRateRatio);
	}

	void handleMixerCallBack(Bitu len);
	void renderingLoop();
};

#endif /* DOSBOX_MIDI_MT32_H */
