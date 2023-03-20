use ndk::audio::{AudioDirection, AudioFormat, AudioStream, AudioStreamBuilder, AudioStreamState};
use ruffle_core::backend::audio::{
    swf, AudioBackend, AudioMixer, DecodeError, RegisterError, SoundHandle, SoundInstanceHandle,
    SoundTransform,
};
use ruffle_core::impl_audio_mixer_backend;

pub struct AAudioAudioBackend {
    pub stream: Option<AudioStream>,
    pub mixer: AudioMixer,
    pub paused: bool,
}

type Error = Box<dyn std::error::Error>;

impl AAudioAudioBackend {
    pub fn new() -> Result<Self, Error> {
        let mixer = AudioMixer::new(2, 44100);

        let mut result = Self {
            stream: None,
            mixer,
            paused: true,
        };

        result.recreate_stream()?;

        Ok(result)
    }

    pub fn recreate_stream(&mut self) -> Result<(), Error> {
        let proxy = self.mixer.proxy();

        let stream = AudioStreamBuilder::new()?
            .direction(AudioDirection::Output)
            .format(AudioFormat::PCM_Float)
            .channel_count(2)
            .sample_rate(44100)
            .performance_mode(ndk::audio::AudioPerformanceMode::LowLatency)
            .data_callback(Box::new(move |_stream, data, len| {
                let sl = unsafe {
                    std::slice::from_raw_parts_mut::<f32>(data as *mut f32, len as usize * 2)
                };
                proxy.mix(sl);
                ndk::audio::AudioCallbackResult::Continue
            }))
            .open_stream()?;

        if !self.paused {
            stream.request_start()?;
        }

        self.stream = Some(stream);
        Ok(())
    }

    pub fn recreate_stream_if_needed(&mut self) {
        let stream_state = self.stream.as_ref().unwrap().get_state().unwrap();
        if stream_state == AudioStreamState::Disconnected {
            // I'm sure it's fine...
            let _ = self.recreate_stream();
        }
    }
}

impl AudioBackend for AAudioAudioBackend {
    impl_audio_mixer_backend!(mixer);

    fn play(&mut self) {
        self.stream
            .as_mut()
            .expect("Error trying to resume audio stream.")
            .request_start()
            .expect("Error trying to resume audio stream.");
        self.paused = false;
    }

    fn pause(&mut self) {
        self.stream
            .as_mut()
            .expect("Error trying to pause audio stream.")
            .request_pause()
            .expect("Error trying to pause audio stream.");
        self.paused = true;
    }
}
