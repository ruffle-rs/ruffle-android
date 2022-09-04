// Yoinked'th from `quad-snd`. Danke sehr, fedor!

use ruffle_core::backend::audio::{
    swf, AudioBackend, AudioMixer, SoundHandle, SoundInstanceHandle, SoundTransform,
};
use ruffle_core::impl_audio_mixer_backend;

pub struct AAudioAudioBackend {
    stream: ndk::aaudio::AAudioStream,
    mixer: AudioMixer,
}

type Error = Box<dyn std::error::Error>;

impl AAudioAudioBackend {
    pub fn new() -> Result<Self, Error> {
        let mixer = AudioMixer::new(2, 44100);
        let pr = mixer.proxy();
        let stream = ndk::aaudio::AAudioStreamBuilder::new()?
            .direction(ndk::aaudio::AAudioDirection::Output)
            .format(ndk::aaudio::AAudioFormat::PCM_I16)
            .channel_count(2)
            .sample_rate(44100)
            .frames_per_data_callback(4096)
            .performance_mode(ndk::aaudio::AAudioPerformanceMode::PowerSaving)
            .data_callback(Box::new(move |_stream, data, len| {
                let mut sl = unsafe {
                    std::slice::from_raw_parts_mut::<i16>(data as *mut i16, len as usize * 2)
                };
                pr.mix(&mut sl);
                ndk::aaudio::AAudioCallbackResult::Continue
            }))
            .open_stream()?;

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
