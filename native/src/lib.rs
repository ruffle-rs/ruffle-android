use jni::objects::ReleaseMode;
use jni::sys::jbyteArray;
use std::rc::Rc;
use std::sync::{Arc, Mutex};
use std::{borrow::Cow, ops::DerefMut};
use winit::{
    event::{DeviceEvent, Event, WindowEvent},
    event_loop::{ControlFlow, EventLoop},
    window::Window,
};

mod audio;

use audio::CpalAudioBackend;

use ruffle_core::{
    backend::{
        log as log_backend, navigator::NullNavigatorBackend, render::NullRenderer,
        storage::MemoryStorageBackend, ui::NullUiBackend, video::SoftwareVideoBackend,
    },
    events::KeyCode,
    tag_utils::SwfMovie,
    Player,
};
use ruffle_render_wgpu::WgpuRenderBackend;
use std::time::Instant;

use winit::event::{
    ElementState, KeyboardInput, ModifiersState, MouseButton, MouseScrollDelta, VirtualKeyCode, TouchPhase,
};

use ruffle_core::events::MouseButton as RuffleMouseButton;
use ruffle_core::events::PlayerEvent;
/// Convert a winit `VirtualKeyCode` into a Ruffle `KeyCode`.
/// Return `KeyCode::Unknown` if there is no matching Flash key code.
fn winit_to_ruffle_key_code(key_code: VirtualKeyCode) -> KeyCode {
    match key_code {
        VirtualKeyCode::Back => KeyCode::Backspace,
        VirtualKeyCode::Tab => KeyCode::Tab,
        VirtualKeyCode::Return => KeyCode::Return,
        VirtualKeyCode::LShift | VirtualKeyCode::RShift => KeyCode::Shift,
        VirtualKeyCode::LControl | VirtualKeyCode::RControl => KeyCode::Control,
        VirtualKeyCode::LAlt | VirtualKeyCode::RAlt => KeyCode::Alt,
        VirtualKeyCode::Capital => KeyCode::CapsLock,
        VirtualKeyCode::Escape => KeyCode::Escape,
        VirtualKeyCode::Space => KeyCode::Space,
        VirtualKeyCode::Key0 => KeyCode::Key0,
        VirtualKeyCode::Key1 => KeyCode::Key1,
        VirtualKeyCode::Key2 => KeyCode::Key2,
        VirtualKeyCode::Key3 => KeyCode::Key3,
        VirtualKeyCode::Key4 => KeyCode::Key4,
        VirtualKeyCode::Key5 => KeyCode::Key5,
        VirtualKeyCode::Key6 => KeyCode::Key6,
        VirtualKeyCode::Key7 => KeyCode::Key7,
        VirtualKeyCode::Key8 => KeyCode::Key8,
        VirtualKeyCode::Key9 => KeyCode::Key9,
        VirtualKeyCode::A => KeyCode::A,
        VirtualKeyCode::B => KeyCode::B,
        VirtualKeyCode::C => KeyCode::C,
        VirtualKeyCode::D => KeyCode::D,
        VirtualKeyCode::E => KeyCode::E,
        VirtualKeyCode::F => KeyCode::F,
        VirtualKeyCode::G => KeyCode::G,
        VirtualKeyCode::H => KeyCode::H,
        VirtualKeyCode::I => KeyCode::I,
        VirtualKeyCode::J => KeyCode::J,
        VirtualKeyCode::K => KeyCode::K,
        VirtualKeyCode::L => KeyCode::L,
        VirtualKeyCode::M => KeyCode::M,
        VirtualKeyCode::N => KeyCode::N,
        VirtualKeyCode::O => KeyCode::O,
        VirtualKeyCode::P => KeyCode::P,
        VirtualKeyCode::Q => KeyCode::Q,
        VirtualKeyCode::R => KeyCode::R,
        VirtualKeyCode::S => KeyCode::S,
        VirtualKeyCode::T => KeyCode::T,
        VirtualKeyCode::U => KeyCode::U,
        VirtualKeyCode::V => KeyCode::V,
        VirtualKeyCode::W => KeyCode::W,
        VirtualKeyCode::X => KeyCode::X,
        VirtualKeyCode::Y => KeyCode::Y,
        VirtualKeyCode::Z => KeyCode::Z,
        VirtualKeyCode::Semicolon => KeyCode::Semicolon,
        VirtualKeyCode::Equals => KeyCode::Equals,
        VirtualKeyCode::Comma => KeyCode::Comma,
        VirtualKeyCode::Minus => KeyCode::Minus,
        VirtualKeyCode::Period => KeyCode::Period,
        VirtualKeyCode::Slash => KeyCode::Slash,
        VirtualKeyCode::Grave => KeyCode::Grave,
        VirtualKeyCode::LBracket => KeyCode::LBracket,
        VirtualKeyCode::Backslash => KeyCode::Backslash,
        VirtualKeyCode::RBracket => KeyCode::RBracket,
        VirtualKeyCode::Apostrophe => KeyCode::Apostrophe,
        VirtualKeyCode::Numpad0 => KeyCode::Numpad0,
        VirtualKeyCode::Numpad1 => KeyCode::Numpad1,
        VirtualKeyCode::Numpad2 => KeyCode::Numpad2,
        VirtualKeyCode::Numpad3 => KeyCode::Numpad3,
        VirtualKeyCode::Numpad4 => KeyCode::Numpad4,
        VirtualKeyCode::Numpad5 => KeyCode::Numpad5,
        VirtualKeyCode::Numpad6 => KeyCode::Numpad6,
        VirtualKeyCode::Numpad7 => KeyCode::Numpad7,
        VirtualKeyCode::Numpad8 => KeyCode::Numpad8,
        VirtualKeyCode::Numpad9 => KeyCode::Numpad9,
        VirtualKeyCode::NumpadMultiply => KeyCode::Multiply,
        VirtualKeyCode::NumpadAdd => KeyCode::Plus,
        VirtualKeyCode::NumpadSubtract => KeyCode::NumpadMinus,
        VirtualKeyCode::NumpadDecimal => KeyCode::NumpadPeriod,
        VirtualKeyCode::NumpadDivide => KeyCode::NumpadSlash,
        VirtualKeyCode::PageUp => KeyCode::PgUp,
        VirtualKeyCode::PageDown => KeyCode::PgDown,
        VirtualKeyCode::End => KeyCode::End,
        VirtualKeyCode::Home => KeyCode::Home,
        VirtualKeyCode::Left => KeyCode::Left,
        VirtualKeyCode::Up => KeyCode::Up,
        VirtualKeyCode::Right => KeyCode::Right,
        VirtualKeyCode::Down => KeyCode::Down,
        VirtualKeyCode::Insert => KeyCode::Insert,
        VirtualKeyCode::Delete => KeyCode::Delete,
        VirtualKeyCode::Pause => KeyCode::Pause,
        VirtualKeyCode::Scroll => KeyCode::ScrollLock,
        VirtualKeyCode::F1 => KeyCode::F1,
        VirtualKeyCode::F2 => KeyCode::F2,
        VirtualKeyCode::F3 => KeyCode::F3,
        VirtualKeyCode::F4 => KeyCode::F4,
        VirtualKeyCode::F5 => KeyCode::F5,
        VirtualKeyCode::F6 => KeyCode::F6,
        VirtualKeyCode::F7 => KeyCode::F7,
        VirtualKeyCode::F8 => KeyCode::F8,
        VirtualKeyCode::F9 => KeyCode::F9,
        VirtualKeyCode::F10 => KeyCode::F10,
        VirtualKeyCode::F11 => KeyCode::F11,
        VirtualKeyCode::F12 => KeyCode::F12,
        _ => KeyCode::Unknown,
    }
}

/// Return a character for the given key code and shift state.
fn winit_key_to_char(key_code: VirtualKeyCode, is_shift_down: bool) -> Option<char> {
    // We need to know the character that a keypress outputs for both key down and key up events,
    // but the winit keyboard API does not provide a way to do this (winit/#753).
    // CharacterReceived events are insufficent because they only fire on key down, not on key up.
    // This is a half-measure to map from keyboard keys back to a character, but does will not work fully
    // for international layouts.
    Some(match (key_code, is_shift_down) {
        (VirtualKeyCode::Space, _) => ' ',
        (VirtualKeyCode::Key0, _) => '0',
        (VirtualKeyCode::Key1, _) => '1',
        (VirtualKeyCode::Key2, _) => '2',
        (VirtualKeyCode::Key3, _) => '3',
        (VirtualKeyCode::Key4, _) => '4',
        (VirtualKeyCode::Key5, _) => '5',
        (VirtualKeyCode::Key6, _) => '6',
        (VirtualKeyCode::Key7, _) => '7',
        (VirtualKeyCode::Key8, _) => '8',
        (VirtualKeyCode::Key9, _) => '9',
        (VirtualKeyCode::A, false) => 'a',
        (VirtualKeyCode::A, true) => 'A',
        (VirtualKeyCode::B, false) => 'b',
        (VirtualKeyCode::B, true) => 'B',
        (VirtualKeyCode::C, false) => 'c',
        (VirtualKeyCode::C, true) => 'C',
        (VirtualKeyCode::D, false) => 'd',
        (VirtualKeyCode::D, true) => 'D',
        (VirtualKeyCode::E, false) => 'e',
        (VirtualKeyCode::E, true) => 'E',
        (VirtualKeyCode::F, false) => 'f',
        (VirtualKeyCode::F, true) => 'F',
        (VirtualKeyCode::G, false) => 'g',
        (VirtualKeyCode::G, true) => 'G',
        (VirtualKeyCode::H, false) => 'h',
        (VirtualKeyCode::H, true) => 'H',
        (VirtualKeyCode::I, false) => 'i',
        (VirtualKeyCode::I, true) => 'I',
        (VirtualKeyCode::J, false) => 'j',
        (VirtualKeyCode::J, true) => 'J',
        (VirtualKeyCode::K, false) => 'k',
        (VirtualKeyCode::K, true) => 'K',
        (VirtualKeyCode::L, false) => 'l',
        (VirtualKeyCode::L, true) => 'L',
        (VirtualKeyCode::M, false) => 'm',
        (VirtualKeyCode::M, true) => 'M',
        (VirtualKeyCode::N, false) => 'n',
        (VirtualKeyCode::N, true) => 'N',
        (VirtualKeyCode::O, false) => 'o',
        (VirtualKeyCode::O, true) => 'O',
        (VirtualKeyCode::P, false) => 'p',
        (VirtualKeyCode::P, true) => 'P',
        (VirtualKeyCode::Q, false) => 'q',
        (VirtualKeyCode::Q, true) => 'Q',
        (VirtualKeyCode::R, false) => 'r',
        (VirtualKeyCode::R, true) => 'R',
        (VirtualKeyCode::S, false) => 's',
        (VirtualKeyCode::S, true) => 'S',
        (VirtualKeyCode::T, false) => 't',
        (VirtualKeyCode::T, true) => 'T',
        (VirtualKeyCode::U, false) => 'u',
        (VirtualKeyCode::U, true) => 'U',
        (VirtualKeyCode::V, false) => 'v',
        (VirtualKeyCode::V, true) => 'V',
        (VirtualKeyCode::W, false) => 'w',
        (VirtualKeyCode::W, true) => 'W',
        (VirtualKeyCode::X, false) => 'x',
        (VirtualKeyCode::X, true) => 'X',
        (VirtualKeyCode::Y, false) => 'y',
        (VirtualKeyCode::Y, true) => 'Y',
        (VirtualKeyCode::Z, false) => 'z',
        (VirtualKeyCode::Z, true) => 'Z',

        (VirtualKeyCode::Semicolon, false) => ';',
        (VirtualKeyCode::Semicolon, true) => ':',
        (VirtualKeyCode::Equals, false) => '=',
        (VirtualKeyCode::Equals, true) => '+',
        (VirtualKeyCode::Comma, false) => ',',
        (VirtualKeyCode::Comma, true) => '<',
        (VirtualKeyCode::Minus, false) => '-',
        (VirtualKeyCode::Minus, true) => '_',
        (VirtualKeyCode::Period, false) => '.',
        (VirtualKeyCode::Period, true) => '>',
        (VirtualKeyCode::Slash, false) => '/',
        (VirtualKeyCode::Slash, true) => '?',
        (VirtualKeyCode::Grave, false) => '`',
        (VirtualKeyCode::Grave, true) => '~',
        (VirtualKeyCode::LBracket, false) => '[',
        (VirtualKeyCode::LBracket, true) => '{',
        (VirtualKeyCode::Backslash, false) => '\\',
        (VirtualKeyCode::Backslash, true) => '|',
        (VirtualKeyCode::RBracket, false) => ']',
        (VirtualKeyCode::RBracket, true) => '}',
        (VirtualKeyCode::Apostrophe, false) => '\'',
        (VirtualKeyCode::Apostrophe, true) => '"',
        (VirtualKeyCode::NumpadMultiply, _) => '*',
        (VirtualKeyCode::NumpadAdd, _) => '+',
        (VirtualKeyCode::NumpadSubtract, _) => '-',
        (VirtualKeyCode::NumpadDecimal, _) => '.',
        (VirtualKeyCode::NumpadDivide, _) => '/',

        (VirtualKeyCode::Numpad0, false) => '0',
        (VirtualKeyCode::Numpad1, false) => '1',
        (VirtualKeyCode::Numpad2, false) => '2',
        (VirtualKeyCode::Numpad3, false) => '3',
        (VirtualKeyCode::Numpad4, false) => '4',
        (VirtualKeyCode::Numpad5, false) => '5',
        (VirtualKeyCode::Numpad6, false) => '6',
        (VirtualKeyCode::Numpad7, false) => '7',
        (VirtualKeyCode::Numpad8, false) => '8',
        (VirtualKeyCode::Numpad9, false) => '9',

        _ => return None,
    })
}

async fn run(event_loop: EventLoop<()>, window: Window) {
    let mut time = Instant::now();
    let mut next_frame_time = Instant::now();

    let mut playerbox: Option<Arc<Mutex<Player>>> = None;

    log::info!("running eventloop");

    event_loop.run(move |event, _, control_flow| {
        *control_flow = ControlFlow::Poll;
        match event {
            Event::WindowEvent { event, .. } => match event {

                WindowEvent::Resized(size) => {
                    let mut player = playerbox.as_ref().unwrap();
                    let mut player_lock = player.lock().unwrap();

                    let viewport_scale_factor = window.scale_factor();

                    player_lock.set_viewport_dimensions(
                        size.width,
                        size.height,
                        viewport_scale_factor,
                    );

                    player_lock
                        .renderer_mut()
                        .set_viewport_dimensions(size.width, size.height);

                    window.request_redraw();
                }

                WindowEvent::Touch(touch) => {
                    log::info!("touch: {:?}", touch);
                    let mut player = playerbox.as_ref().unwrap();

                    let mut player_lock = player.lock().unwrap();
                    let x = touch.location.x;
                    let y = touch.location.y;

                    let button = RuffleMouseButton::Left;

                    if touch.phase == TouchPhase::Started {
                        let event = PlayerEvent::MouseMove { x, y };
                        player_lock.handle_event(event);
                        let event = PlayerEvent::MouseDown { x, y, button };
                        player_lock.handle_event(event);
                    }
                    if touch.phase == TouchPhase::Moved {
                        let event = PlayerEvent::MouseMove { x, y };
                        player_lock.handle_event(event);
                    }
                    if touch.phase == TouchPhase::Ended || touch.phase == TouchPhase::Cancelled {
                        let event = PlayerEvent::MouseUp { x, y, button };
                        player_lock.handle_event(event);
                    }

                    if player_lock.needs_render() {
                        window.request_redraw();
                    }
                }

                WindowEvent::KeyboardInput { input, .. } => {
                    let mut player = playerbox.as_ref().unwrap();

                    log::info!("keyboard input: {:?}", input);

                    let mut player_lock = player.lock().unwrap();
                    if let Some(key) = input.virtual_keycode {
                        let key_code = winit_to_ruffle_key_code(key);
                        let key_char =
                            winit_key_to_char(key, input.modifiers.contains(ModifiersState::SHIFT));
                        let event = match input.state {
                            ElementState::Pressed => PlayerEvent::KeyDown { key_code, key_char },
                            ElementState::Released => PlayerEvent::KeyUp { key_code, key_char },
                        };
                        log::info!("Ruffle key event: {:?}", event);
                        player_lock.handle_event(event);

                        // NOTE: this is a HACK
                        if let Some(key) = key_char {
                            let event = PlayerEvent::TextInput { codepoint: key };
                            log::info!("faking text input: {:?}", key);
                            player_lock.handle_event(event);
                        }

                        if player_lock.needs_render() {
                            window.request_redraw();
                        }
                    }
                }

                // NOTE: this never happens at the moment
                WindowEvent::ReceivedCharacter(codepoint) => {
                    log::info!("keyboard character: {:?}", codepoint);
                    let mut player = playerbox.as_ref().unwrap();
                    let mut player_lock = player.lock().unwrap();

                    let event = PlayerEvent::TextInput { codepoint };
                    player_lock.handle_event(event);
                    if player_lock.needs_render() {
                        window.request_redraw();
                    }
                }

                _ => {}
            },

            Event::DeviceEvent { device_id, event } => {
                log::info!("device event: {:?}", event);
                match event {
                    DeviceEvent::Key(key) => {
                        log::info!("key: {:?}", key);
                    }
                    _ => {}
                }
            }
            Event::Resumed => {
                println!("resume");
                log::info!("RUFFLE RESUMED");

                if playerbox.is_none() {
                    let size = window.inner_size();

                    let renderer = Box::new(
                        WgpuRenderBackend::for_window(
                            &window,
                            (window.inner_size().width, window.inner_size().height),
                            wgpu::Backends::all(),
                            wgpu::PowerPreference::HighPerformance,
                            None,
                        )
                        .unwrap(),
                    );

                    let start = std::time::Instant::now();
                    let log = Box::new(log_backend::NullLogBackend::new());
                    let audio = Box::new(CpalAudioBackend::new().unwrap());
                    let navigator = Box::new(NullNavigatorBackend::new());
                    let storage = Box::new(MemoryStorageBackend::default());
                    let video = Box::new(SoftwareVideoBackend::new());
                    let ui = Box::new(NullUiBackend::new());

                    playerbox = Some(
                        Player::new(renderer, audio, navigator, storage, video, log, ui).unwrap(),
                    );

                    let mut player = playerbox.as_ref().unwrap();
                    let mut player_lock = player.lock().unwrap();

                    match get_swf_bytes() {
                        Ok(bytes) => {
                            let movie = SwfMovie::from_data(&bytes, None, None).unwrap();

                            player_lock.set_root_movie(Arc::new(movie));
                            player_lock.set_is_playing(true); // Desktop player will auto-play.


                            let viewport_size = window.inner_size();
                            let viewport_scale_factor = window.scale_factor();
                            player_lock.set_letterbox(ruffle_core::config::Letterbox::On);

                            log::info!("VIEWP SIZE: {:?}", viewport_size);

                            player_lock.set_viewport_dimensions(
                                viewport_size.width,
                                viewport_size.height,
                                viewport_scale_factor,
                            );

                            time = Instant::now();
                            next_frame_time = Instant::now();

                            log::info!("MOVIE STARTED");
                        }
                        Err(e) => {
                            log::error!("{}", e);
                        }
                    }
                }
            }
            Event::Suspended => {
                println!("suspend");
            }
            Event::MainEventsCleared => {
                let new_time = Instant::now();
                let dt = new_time.duration_since(time).as_micros();

                if dt > 0 {
                    time = new_time;
                    if playerbox.is_some() {
                        let mut player = playerbox.as_ref().unwrap();

                        let mut player_lock = player.lock().unwrap();
                        player_lock.tick(dt as f64 / 1000.0);
                        //log::info!("RUFFLE TICKED");
                        next_frame_time = new_time + player_lock.time_til_next_frame();

                        if player_lock.needs_render() {
                            window.request_redraw();
                            //log::info!("REQUESTED REDRAW");
                        }
                    }
                }
            }

            // Render
            Event::RedrawRequested(_) => {
                //log::info!("REDRAWING");
                // TODO: Don't render when minimized to avoid potential swap chain errors in `wgpu`.
                // TODO: also disable when suspended!

                if playerbox.is_some() {
                    let mut player = playerbox.as_ref().unwrap();

                    let mut player_lock = player.lock().unwrap();
                    player_lock.render();
                    //log::info!("RUFFLE RENDERED");
                }
            }

            Event::WindowEvent {
                event: WindowEvent::CloseRequested,
                ..
            } => *control_flow = ControlFlow::Exit,

            _ => {}
        }

        // After polling events, sleep the event loop until the next event or the next frame.
        if *control_flow != ControlFlow::Exit {
            *control_flow = ControlFlow::WaitUntil(next_frame_time);
        }
    });
}

fn get_swf_bytes() -> Result<Vec<u8>, Box<dyn std::error::Error>> {
    // Create a VM for executing Java calls
    let ctx = ndk_context::android_context();
    let vm = unsafe { jni::JavaVM::from_raw(ctx.vm().cast()) }?;
    let env = vm.attach_current_thread()?;

    // no worky :(
    ndk_glue::native_activity().show_soft_input(true);

    // The following is basically equivalent to this Java code:
    /*
    Intent intent = getIntent();
    Bundle extras = intent.getExtras();
    Uri uri = extras.get("SWF_URI");

    ContentResolver resolver = getContentResolver();
    InputStream inputStream = resolver.openInputStream(uri);

    int available = inputStream.available();
    byte[] bytes = new byte[available];
    // assuming the whole contents will be available at once
    int _num_bytes_read = inputStream.read(bytes);
    */

    let intent = env.call_method(
        ctx.context() as jni::sys::jobject,
        "getIntent",
        "()Landroid/content/Intent;",
        &[],
    )?;
    let extras = env.call_method(intent.l()?, "getExtras", "()Landroid/os/Bundle;", &[])?;
    let uri = env.call_method(
        extras.l()?,
        "get",
        "(Ljava/lang/String;)Ljava/lang/Object;",
        &[jni::objects::JValue::Object(
            env.new_string("SWF_URI")?.into(),
        )],
    )?;

    let resolver = env.call_method(
        ctx.context() as jni::sys::jobject,
        "getContentResolver",
        "()Landroid/content/ContentResolver;",
        &[],
    )?;
    let stream = env.call_method(
        resolver.l()?,
        "openInputStream",
        "(Landroid/net/Uri;)Ljava/io/InputStream;",
        &[jni::objects::JValue::Object(uri.l()?)],
    )?;

    let available = env.call_method(stream.l()?, "available", "()I", &[])?;
    let bytes = env.new_byte_array(available.i()?)?;
    let _num_bytes_read = env.call_method(
        stream.l()?,
        "read",
        "([B)I",
        &[jni::objects::JValue::Object(jni::objects::JObject::from(
            bytes,
        ))],
    )?;

    // And finally getting the bytes into a Vec
    let elements = env.get_byte_array_elements(bytes as jbyteArray, ReleaseMode::NoCopyBack)?;
    unsafe {
        let data =
            std::slice::from_raw_parts(elements.as_ptr() as *mut u8, elements.size()? as usize);
        Ok(data.to_vec())
    }
}

#[cfg_attr(
    target_os = "android",
    ndk_glue::main(backtrace = "on", logger(level = "info", tag = "ruffle"))
)]
fn main() {
    log::info!("start");
    let event_loop = EventLoop::new();
    log::info!("got eventloop");
    let window = winit::window::Window::new(&event_loop).unwrap();
    window.inner_size();
    log::info!("got window");

    pollster::block_on(run(event_loop, window));
}
