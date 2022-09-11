use ndk::audio::{AudioError, AudioErrorResult, AudioStream};
use ruffle_core::backend::audio::{
    swf, AudioBackend, AudioMixer, AudioMixerProxy, DecodeError, RegisterError, SoundHandle,
    SoundInstanceHandle, SoundTransform,
};
use ruffle_core::impl_audio_mixer_backend;

pub struct AAudioAudioBackend {
    stream: ndk::audio::AudioStream,
    mixer: AudioMixer,
}

type Error = Box<dyn std::error::Error>;

impl AAudioAudioBackend {
    fn make_stream(mixer: &AudioMixer) -> Result<AudioStream, Error> {
        let pr = mixer.proxy();
        ndk::audio::AudioStreamBuilder::new()?
            .direction(ndk::audio::AudioDirection::Output)
            .format(ndk::audio::AudioFormat::PCM_I16)
            .channel_count(2)
            .sample_rate(44100)
            .performance_mode(ndk::audio::AudioPerformanceMode::LowLatency)
            .data_callback(Box::new(move |_stream, data, len| {
                let mut sl = unsafe {
                    std::slice::from_raw_parts_mut::<i16>(data as *mut i16, len as usize * 2)
                };
                pr.mix(&mut sl);
                ndk::audio::AudioCallbackResult::Continue
            }))
            .error_callback(Box::new(move |&mut stream, error| {
                if error == AudioError::ErrorResult(AudioErrorResult::Disconnected) {
                    stream.request_stop();
                    stream = Self::make_stream(mixer);
                }
            }))
            .open_stream()
    }

    pub fn new() -> Result<Self, Error> {
        let mixer = AudioMixer::new(2, 44100);

        let stream = Self::make_stream(&mixer)?;
        Ok(Self { stream, mixer })
    }
}

impl AudioBackend for AAudioAudioBackend {
    impl_audio_mixer_backend!(mixer);

    fn play(&mut self) {
        self.stream
            .request_start()
            .expect("Error trying to resume audio stream.");
    }

    fn pause(&mut self) {
        self.stream
            .request_pause()
            .expect("Error trying to pause audio stream.");
    }
}
