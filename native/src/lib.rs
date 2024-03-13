mod audio;
mod custom_event;
mod executor;
mod keycodes;
mod navigator;
mod task;

use custom_event::RuffleEvent;

use jni::{
    objects::{JByteArray, JClass, JIntArray, JObject, ReleaseMode},
    sys,
    sys::{jbyte, jchar, jint, jobject},
    JNIEnv, JavaVM,
};
use std::sync::mpsc::{Receiver, Sender, TryRecvError};
use std::sync::{mpsc, MutexGuard};
use std::time::Duration;
use std::{
    sync::{Arc, Mutex},
    time::Instant,
};
use wgpu::rwh::{AndroidDisplayHandle, HasDisplayHandle, HasWindowHandle, RawDisplayHandle};

use android_activity::input::{Button, InputEvent, KeyAction, MotionAction};
use android_activity::{AndroidApp, AndroidAppWaker, InputStatus, MainEvent, PollEvent};
use jni::objects::JValue;

use audio::AAudioAudioBackend;
use navigator::ExternalNavigatorBackend;
use ruffle_core::backend::storage::MemoryStorageBackend;
use url::Url;

use executor::WinitAsyncExecutor;

use ruffle_core::events::MouseButton;
use ruffle_core::{
    events::{KeyCode, MouseButton as RuffleMouseButton, PlayerEvent},
    tag_utils::SwfMovie,
    Player, PlayerBuilder, ViewportDimensions,
};

use crate::keycodes::android_keycode_to_ruffle;
use ruffle_render_wgpu::{backend::WgpuRenderBackend, target::SwapChainTarget};

/// Represents a current Player and any associated state with that player,
/// which may be lost when this Player is closed (dropped)
struct ActivePlayer {
    player: Arc<Mutex<Player>>,
    executor: Arc<Mutex<WinitAsyncExecutor>>,
}

#[derive(Clone)]
pub struct EventSender {
    sender: Sender<RuffleEvent>,
    waker: AndroidAppWaker,
}

impl EventSender {
    pub fn send(&self, event: RuffleEvent) {
        if self.sender.send(event).is_ok() {
            self.waker.wake();
        }
    }
}

fn run(app: AndroidApp) {
    let mut last_frame_time = Instant::now();
    let mut next_frame_time = Some(Instant::now());
    let mut quit = false;
    let (sender, receiver) = mpsc::channel::<RuffleEvent>();
    let mut native_window: Option<ndk::native_window::NativeWindow> = None;
    let mut playerbox: Option<ActivePlayer> = None;
    let sender = EventSender {
        sender,
        waker: app.create_waker(),
    };

    log::info!("Starting event loop...");

    unsafe {
        let vm = JavaVM::from_raw(app.vm_as_ptr() as *mut sys::JavaVM).expect("JVM must exist");
        let activity = JObject::from_raw(app.activity_as_ptr() as jobject);
        let _ = vm
            .get_env()
            .unwrap()
            .set_rust_field(activity, "eventLoopHandle", sender.clone());
    }

    while !quit {
        let mut needs_redraw = false;
        app.poll_events(
            Some(
                next_frame_time
                    .and_then(|next| next.checked_duration_since(last_frame_time))
                    .unwrap_or_else(|| Duration::from_millis(100)),
            ),
            |event| {
                match event {
                    PollEvent::Main(event) => match event {
                        MainEvent::Destroy => {
                            quit = true;
                        }
                        MainEvent::WindowResized { .. } => {
                            if let Some(player) = playerbox.as_ref() {
                                let mut player_lock = player.player.lock().unwrap();
                                let window = native_window
                                    .as_ref()
                                    .expect("native_window should be Some for a WindowResized");
                                let viewport_scale_factor = app
                                    .config()
                                    .density()
                                    .map(|dpi| dpi as f64 / 160.0)
                                    .unwrap_or(1.0);
                                let dimensions = ViewportDimensions {
                                    width: window.width() as u32,
                                    height: window.height() as u32,
                                    scale_factor: viewport_scale_factor,
                                };
                                player_lock.set_viewport_dimensions(dimensions);
                                needs_redraw = true;
                            }
                        }
                        MainEvent::InitWindow { .. } => {
                            native_window = app.native_window();
                            let window = native_window
                                .as_ref()
                                .expect("native_window should be Some after InitWindow");
                            let viewport_scale_factor = app
                                .config()
                                .density()
                                .map(|dpi| dpi as f64 / 160.0)
                                .unwrap_or(1.0);
                            let dimensions = ViewportDimensions {
                                width: window.width() as u32,
                                height: window.height() as u32,
                                scale_factor: viewport_scale_factor,
                            };

                            let renderer = unsafe {
                                // TODO: make this take an Arc<Window> instead?
                                WgpuRenderBackend::for_window_unsafe(
                                    wgpu::SurfaceTargetUnsafe::RawHandle {
                                        raw_display_handle: RawDisplayHandle::Android(
                                            AndroidDisplayHandle::new(),
                                        ),
                                        raw_window_handle: window.window_handle().unwrap().into(),
                                    },
                                    (dimensions.width, dimensions.height),
                                    wgpu::Backends::GL,
                                    wgpu::PowerPreference::HighPerformance,
                                    None,
                                )
                                .unwrap()
                            };
                            let movie_url = Url::parse("file://movie.swf").unwrap();

                            let (executor, channel) = WinitAsyncExecutor::new(
                                sender.clone(), /*, app.create_waker()*/
                            );
                            let navigator = ExternalNavigatorBackend::new(
                                movie_url.clone(),
                                channel,
                                sender.clone(),
                                // app.create_waker(),
                                true,
                                ruffle_core::backend::navigator::OpenURLMode::Allow,
                            );

                            playerbox = Some(ActivePlayer {
                                player: PlayerBuilder::new()
                                    .with_renderer(renderer)
                                    .with_audio(AAudioAudioBackend::new().unwrap())
                                    .with_storage(MemoryStorageBackend::default())
                                    .with_navigator(navigator)
                                    .with_video(
                                        ruffle_video_software::backend::SoftwareVideoBackend::new(),
                                    )
                                    .build(),
                                executor: executor,
                            });

                            let player = &playerbox.as_ref().unwrap().player;
                            let mut player_lock = player.lock().unwrap();

                            match get_swf_bytes() {
                                Ok(bytes) => {
                                    let movie = SwfMovie::from_data(
                                        &bytes,
                                        "file://movie.swf".to_string(),
                                        None,
                                    )
                                    .unwrap();

                                    player_lock.mutate_with_update_context(|context| {
                                        context.set_root_movie(movie);
                                    });
                                    player_lock.set_is_playing(true); // Desktop player will auto-play.

                                    player_lock.set_letterbox(ruffle_core::config::Letterbox::On);

                                    player_lock.set_viewport_dimensions(dimensions);

                                    last_frame_time = Instant::now();
                                    next_frame_time = Some(Instant::now());

                                    log::info!("MOVIE STARTED");
                                }
                                Err(e) => {
                                    log::error!("{}", e);
                                }
                            }
                        }
                        MainEvent::InputAvailable => {
                            if let Ok(mut inputs) = app.input_events_iter() {
                                while inputs.next(|input| match input {
                                    InputEvent::MotionEvent(event) => {
                                        let window = native_window.as_ref().unwrap();
                                        let pointer = event.pointer_index();
                                        let pointer = event.pointer_at_index(pointer);
                                        let coords: (i32, i32) = get_loc_on_screen().unwrap();
                                        let mut x = pointer.x() as f64 - coords.0 as f64;
                                        let mut y = pointer.y() as f64 - coords.1 as f64;
                                        let view_size = get_view_size().unwrap();
                                        x = x * window.width() as f64 / view_size.0 as f64;
                                        y = y * window.height() as f64 / view_size.1 as f64;
                                        let ruffle_event = match event.action() {
                                            MotionAction::Down | MotionAction::PointerDown => {
                                                PlayerEvent::MouseDown {
                                                    x: x,
                                                    y: y,
                                                    button: MouseButton::Left, // TODO
                                                }
                                            }
                                            MotionAction::Up | MotionAction::PointerUp => {
                                                PlayerEvent::MouseUp {
                                                    x: x,
                                                    y: y,
                                                    button: MouseButton::Left, // TODO
                                                }
                                            }
                                            MotionAction::Move => {
                                                PlayerEvent::MouseMove { x: x, y: y }
                                            }
                                            _ => return InputStatus::Unhandled,
                                        };

                                        if let Some(player) = playerbox.as_ref() {
                                            player
                                                .player
                                                .lock()
                                                .unwrap()
                                                .handle_event(ruffle_event);
                                        }

                                        InputStatus::Handled
                                    }
                                    InputEvent::KeyEvent(event) => {
                                        if let Some(player) = playerbox.as_ref() {
                                            let Some(key_code) =
                                                android_keycode_to_ruffle(event.key_code())
                                            else {
                                                return InputStatus::Unhandled;
                                            };
                                            let ruffle_event = match event.action() {
                                                KeyAction::Down => PlayerEvent::KeyDown {
                                                    key_code,
                                                    key_char: None,
                                                },
                                                KeyAction::Up => PlayerEvent::KeyUp {
                                                    key_code,
                                                    key_char: None,
                                                },
                                                _ => return InputStatus::Unhandled,
                                            };
                                            player
                                                .player
                                                .lock()
                                                .unwrap()
                                                .handle_event(ruffle_event);
                                            needs_redraw = true;
                                        }

                                        InputStatus::Handled
                                    }
                                    _ => InputStatus::Unhandled,
                                }) {}
                            }
                        }
                        _ => {} // Something else happened but it's probably not important for now.
                    },
                    _ => {} // We got woken up, or we timedout (no events happened)
                }
            },
        );

        match receiver.try_recv() {
            Err(_) => {}
            Ok(RuffleEvent::Resize(size)) => {
                if let Some(player) = playerbox.as_ref() {
                    player.player.lock().unwrap().set_viewport_dimensions(size);
                    needs_redraw = true;
                }
            }
            Ok(RuffleEvent::TaskPoll) => {
                if let Some(player) = playerbox.as_ref() {
                    player
                        .executor
                        .lock()
                        .expect("Executor lock must be available")
                        .poll_all()
                }
            }
            Ok(RuffleEvent::VirtualKeyEvent {
                down,
                key_code,
                key_char,
            }) => {
                if let Some(player) = playerbox.as_ref() {
                    let event = if down {
                        PlayerEvent::KeyDown { key_code, key_char }
                    } else {
                        PlayerEvent::KeyUp { key_code, key_char }
                    };
                    player.player.lock().unwrap().handle_event(event);
                    if down {
                        // NOTE: this is a HACK
                        if let Some(key) = key_char {
                            let event = PlayerEvent::TextInput { codepoint: key };
                            log::info!("faking text input: {:?}", key);
                            player.player.lock().unwrap().handle_event(event);
                        }
                    }
                }
            }
            Ok(RuffleEvent::RunContextMenuCallback(index)) => {
                if let Some(player) = playerbox.as_ref() {
                    player
                        .player
                        .lock()
                        .unwrap()
                        .run_context_menu_callback(index);
                }
            }
            Ok(RuffleEvent::ClearContextMenu) => {
                if let Some(player) = playerbox.as_ref() {
                    player.player.lock().unwrap().clear_custom_menu_items();
                }
            }
            Ok(RuffleEvent::RequestContextMenu) => {
                if let Some(player) = playerbox.as_ref() {
                    log::warn!("preparing context menu!");
                    let items = player.player.lock().unwrap().prepare_context_menu();
                    let (jvm, activity) = get_jvm().unwrap();
                    let mut env = jvm.attach_current_thread().unwrap();
                    let arr = env
                        .new_object_array(items.len() as i32, "java/lang/String", JObject::null())
                        .unwrap();
                    for (i, e) in items.iter().enumerate() {
                        let s = env
                            .new_string(&format!(
                                "{} {} {} {}",
                                e.enabled, e.separator_before, e.checked, e.caption
                            ))
                            .unwrap();
                        env.set_object_array_element(&arr, i as i32, s);
                    }
                    let _ = env.call_method(
                        activity,
                        "showContextMenu",
                        "([Ljava/lang/String;)V",
                        &[JValue::Object(&arr)],
                    );
                }
            }
        }

        let new_time = Instant::now();
        let dt = new_time.duration_since(last_frame_time).as_micros();
        if dt > 0 {
            last_frame_time = new_time;
            if let Some(player) = playerbox.as_ref() {
                if let Ok(mut player) = player.player.lock() {
                    player.tick(dt as f64 / 1000.0);
                    next_frame_time = Some(new_time + player.time_til_next_frame());
                    needs_redraw = player.needs_render();
                    let audio = player
                        .audio_mut()
                        .downcast_mut::<AAudioAudioBackend>()
                        .unwrap();
                    audio.recreate_stream_if_needed();
                }
            } else {
                next_frame_time = None;
            }
        }

        if needs_redraw {
            if let Some(player) = playerbox.as_ref() {
                if let Ok(mut player) = player.player.lock() {
                    player.render();
                }
            }
        }
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_rs_ruffle_FullscreenNativeActivity_keydown(
    mut env: JNIEnv,
    this: JObject,
    key_code_raw: jbyte,
    key_char_raw: jchar,
) {
    let event_loop: MutexGuard<Sender<RuffleEvent>> =
        env.get_rust_field(this, "eventLoopHandle").unwrap();
    let key_code: KeyCode = ::std::mem::transmute(key_code_raw);
    let key_char = std::char::from_u32(key_char_raw as u32);
    let _ = event_loop.send(RuffleEvent::VirtualKeyEvent {
        down: true,
        key_code,
        key_char,
    });
}

#[no_mangle]
pub unsafe extern "C" fn Java_rs_ruffle_FullscreenNativeActivity_keyup(
    mut env: JNIEnv,
    this: JObject,
    key_code_raw: jbyte,
    key_char_raw: jchar,
) {
    let event_loop: MutexGuard<Sender<RuffleEvent>> =
        env.get_rust_field(this, "eventLoopHandle").unwrap();
    let key_code: KeyCode = ::std::mem::transmute(key_code_raw);
    let key_char = std::char::from_u32(key_char_raw as u32);
    let _ = event_loop.send(RuffleEvent::VirtualKeyEvent {
        down: false,
        key_code,
        key_char,
    });
}

#[no_mangle]
pub unsafe extern "C" fn Java_rs_ruffle_FullscreenNativeActivity_resized(
    mut env: JNIEnv,
    this: JObject,
) {
    let event_loop: MutexGuard<Sender<RuffleEvent>> =
        env.get_rust_field(this, "eventLoopHandle").unwrap();
    let size = get_view_size();
    if let Ok((w, h)) = size {
        let viewport_scale_factor = 1.0; //window.scale_factor();
        let _ = event_loop.send(RuffleEvent::Resize(ViewportDimensions {
            width: w as u32,
            height: h as u32,
            scale_factor: viewport_scale_factor,
        }));
    }
    log::warn!("resized!");
}

pub fn get_jvm<'a>() -> Result<(jni::JavaVM, JObject<'a>), Box<dyn std::error::Error>> {
    // Create a VM for executing Java calls
    let context = ndk_context::android_context();
    let activity = unsafe { JObject::from_raw(context.context().cast()) };
    let vm = unsafe { jni::JavaVM::from_raw(context.vm().cast()) }?;

    Ok((vm, activity))
}

#[no_mangle]
pub unsafe extern "C" fn Java_rs_ruffle_FullscreenNativeActivity_requestContextMenu(
    mut env: JNIEnv,
    this: JObject,
) {
    let event_loop: MutexGuard<Sender<RuffleEvent>> =
        env.get_rust_field(this, "eventLoopHandle").unwrap();
    let _ = event_loop.send(RuffleEvent::RequestContextMenu);
}

#[no_mangle]
pub unsafe extern "C" fn Java_rs_ruffle_FullscreenNativeActivity_runContextMenuCallback(
    mut env: JNIEnv,
    this: JObject,
    index: jint,
) {
    let event_loop: MutexGuard<Sender<RuffleEvent>> =
        env.get_rust_field(this, "eventLoopHandle").unwrap();
    let _ = event_loop.send(RuffleEvent::RunContextMenuCallback(index as usize));
}

#[no_mangle]
pub unsafe extern "C" fn Java_rs_ruffle_FullscreenNativeActivity_clearContextMenu(
    mut env: JNIEnv,
    this: JObject,
) {
    let event_loop: MutexGuard<Sender<RuffleEvent>> =
        env.get_rust_field(this, "eventLoopHandle").unwrap();
    let _ = event_loop.send(RuffleEvent::ClearContextMenu);
}

fn get_swf_bytes() -> Result<Vec<u8>, Box<dyn std::error::Error>> {
    let (jvm, activity) = get_jvm()?;
    let mut env = jvm.attach_current_thread()?;

    // no worky :(
    //ndk_glue::native_activity().show_soft_input(true);

    let bytes = env.call_method(&activity, "getSwfBytes", "()[B", &[])?;
    let arr = JByteArray::from(bytes.l()?);
    let elements = unsafe { env.get_array_elements(&arr, ReleaseMode::NoCopyBack)? };
    let data = unsafe { std::slice::from_raw_parts(elements.as_ptr() as *mut u8, elements.len()) };

    Ok(data.to_vec())
}

fn get_loc_on_screen() -> Result<(i32, i32), Box<dyn std::error::Error>> {
    let (jvm, activity) = get_jvm()?;
    let mut env = jvm.attach_current_thread()?;

    // no worky :(
    //ndk_glue::native_activity().show_soft_input(true);

    let loc = env.call_method(&activity, "getLocOnScreen", "()[I", &[])?;
    let arr = JIntArray::from(loc.l()?);
    let elements = unsafe { env.get_array_elements(&arr, ReleaseMode::NoCopyBack) }?;

    let coords =
        unsafe { std::slice::from_raw_parts(elements.as_ptr() as *mut i32, elements.len()) };
    Ok((coords[0], coords[1]))
}

fn get_view_size() -> Result<(i32, i32), Box<dyn std::error::Error>> {
    let (jvm, activity) = get_jvm()?;
    let mut env = jvm.attach_current_thread()?;

    let width = env.call_method(&activity, "getSurfaceWidth", "()I", &[])?;
    let height = env.call_method(&activity, "getSurfaceHeight", "()I", &[])?;

    Ok((width.i().unwrap(), height.i().unwrap()))
}

#[no_mangle]
fn android_main(app: AndroidApp) {
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Info)
            .with_tag("ruffle")
            .with_filter(
                android_logger::FilterBuilder::new()
                    .parse("warn,ruffle=info")
                    .build(),
            ),
    );

    log_panics::init();

    log::info!("Starting android_main...");
    run(app);
}
