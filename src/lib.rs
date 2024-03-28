mod audio;
mod custom_event;
mod executor;
mod java;
mod keycodes;
mod navigator;
mod task;
mod trace;

use custom_event::RuffleEvent;

use jni::{
    objects::JObject,
    sys,
    sys::{jbyte, jchar, jint, jobject},
    JNIEnv, JavaVM,
};
use std::sync::mpsc::Sender;
use std::sync::{mpsc, MutexGuard};
use std::time::Duration;
use std::{
    sync::{Arc, Mutex},
    time::Instant,
};
use wgpu::rwh::{AndroidDisplayHandle, HasWindowHandle, RawDisplayHandle};

use android_activity::input::{InputEvent, KeyAction, MotionAction};
use android_activity::{AndroidApp, AndroidAppWaker, InputStatus, MainEvent, PollEvent};
use jni::objects::JClass;

use audio::AAudioAudioBackend;
use navigator::ExternalNavigatorBackend;
use ruffle_core::backend::storage::MemoryStorageBackend;
use url::Url;

use executor::NativeAsyncExecutor;

use ruffle_core::{
    events::{KeyCode, MouseButton, PlayerEvent},
    tag_utils::SwfMovie,
    Player, PlayerBuilder, ViewportDimensions,
};

use crate::keycodes::android_keycode_to_ruffle;
use crate::trace::FileLogBackend;
use java::JavaInterface;
use ruffle_render_wgpu::{backend::WgpuRenderBackend, target::SwapChainTarget};

/// Represents a current Player and any associated state with that player,
/// which may be lost when this Player is closed (dropped)
struct ActivePlayer {
    player: Arc<Mutex<Player>>,
    executor: Arc<Mutex<NativeAsyncExecutor>>,
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
    let trace_output;

    unsafe {
        let vm = JavaVM::from_raw(app.vm_as_ptr() as *mut sys::JavaVM).expect("JVM must exist");
        let activity = JObject::from_raw(app.activity_as_ptr() as jobject);
        let mut jni_env = vm.get_env().unwrap();
        trace_output = JavaInterface::get_trace_output(&mut jni_env, &activity);
        let _ = jni_env.set_rust_field(activity, "eventLoopHandle", sender.clone());
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
                                log::info!(
                                    "WindowResized: {} x {}",
                                    window.width(),
                                    window.height()
                                );
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
                        MainEvent::Resume { .. } => {
                            if let Some(player) = playerbox.as_ref() {
                                if let Some(window) = native_window.as_ref() {
                                    // [NA] For some reason we can get negative sizes during a resume...
                                    if window.width() > 0 && window.height() > 0 {
                                        unsafe {
                                            player
                                                .player
                                                .lock()
                                                .unwrap()
                                                .renderer_mut()
                                                .downcast_mut::<WgpuRenderBackend<SwapChainTarget>>(
                                                )
                                                .unwrap()
                                                .recreate_surface_unsafe(
                                                    wgpu::SurfaceTargetUnsafe::RawHandle {
                                                        raw_display_handle:
                                                            RawDisplayHandle::Android(
                                                                AndroidDisplayHandle::new(),
                                                            ),
                                                        raw_window_handle: window
                                                            .window_handle()
                                                            .unwrap()
                                                            .into(),
                                                    },
                                                    (window.width() as u32, window.height() as u32),
                                                )
                                                .unwrap();
                                        }
                                    }
                                }
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
                            log::info!(
                                "Init window: {} x {} (is existing: {})",
                                window.width(),
                                window.height(),
                                playerbox.is_some()
                            );

                            if playerbox.is_none() {
                                let renderer = unsafe {
                                    // TODO: make this take an Arc<Window> instead?
                                    WgpuRenderBackend::for_window_unsafe(
                                        wgpu::SurfaceTargetUnsafe::RawHandle {
                                            raw_display_handle: RawDisplayHandle::Android(
                                                AndroidDisplayHandle::new(),
                                            ),
                                            raw_window_handle: window
                                                .window_handle()
                                                .unwrap()
                                                .into(),
                                        },
                                        (dimensions.width, dimensions.height),
                                        wgpu::Backends::GL,
                                        wgpu::PowerPreference::HighPerformance,
                                        None,
                                    )
                                    .unwrap()
                                };
                                let movie_url = Url::parse("file://movie.swf").unwrap();

                                let (executor, channel) = NativeAsyncExecutor::new(
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
                                    .with_log(FileLogBackend::new(trace_output.as_deref()))
                                    .with_video(
                                        ruffle_video_software::backend::SoftwareVideoBackend::new(),
                                    )
                                    .build(),
                                executor,
                            });

                                let player = &playerbox.as_ref().unwrap().player;
                                let mut player_lock = player.lock().unwrap();
                                let (jvm, activity) = get_jvm().unwrap();
                                let mut env = jvm.attach_current_thread().unwrap();
                                let url = JavaInterface::get_swf_uri(&mut env, &activity);
                                let bytes = JavaInterface::get_swf_bytes(&mut env, &activity);

                                if let Some(bytes) = bytes {
                                    let movie = SwfMovie::from_data(&bytes, url, None).unwrap();
                                    player_lock.mutate_with_update_context(|context| {
                                        context.set_root_movie(movie);
                                    });
                                } else {
                                    player_lock.fetch_root_movie(url, Vec::new(), Box::new(|_| {}))
                                }
                                player_lock.set_is_playing(true); // Desktop player will auto-play.

                                player_lock.set_letterbox(ruffle_core::config::Letterbox::On);

                                player_lock.set_viewport_dimensions(dimensions);

                                last_frame_time = Instant::now();
                                next_frame_time = Some(Instant::now());

                                log::info!("MOVIE STARTED");
                            } else {
                                let player = &playerbox.as_ref().unwrap().player;
                                let mut player_lock = player.lock().unwrap();
                                unsafe {
                                    player_lock
                                        .renderer_mut()
                                        .downcast_mut::<WgpuRenderBackend<SwapChainTarget>>()
                                        .unwrap()
                                        .recreate_surface_unsafe(
                                            wgpu::SurfaceTargetUnsafe::RawHandle {
                                                raw_display_handle: RawDisplayHandle::Android(
                                                    AndroidDisplayHandle::new(),
                                                ),
                                                raw_window_handle: window
                                                    .window_handle()
                                                    .unwrap()
                                                    .into(),
                                            },
                                            (window.width() as u32, window.height() as u32),
                                        )
                                        .unwrap();
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
                                        let coords: (i32, i32) = get_loc_on_screen();
                                        let mut x = pointer.x() as f64 - coords.0 as f64;
                                        let mut y = pointer.y() as f64 - coords.1 as f64;
                                        let view_size = get_view_size().unwrap();
                                        x = x * window.width() as f64 / view_size.0 as f64;
                                        y = y * window.height() as f64 / view_size.1 as f64;
                                        let ruffle_event = match event.action() {
                                            MotionAction::Down | MotionAction::PointerDown => {
                                                PlayerEvent::MouseDown {
                                                    x,
                                                    y,
                                                    button: MouseButton::Left, // TODO
                                                }
                                            }
                                            MotionAction::Up | MotionAction::PointerUp => {
                                                PlayerEvent::MouseUp {
                                                    x,
                                                    y,
                                                    button: MouseButton::Left, // TODO
                                                }
                                            }
                                            MotionAction::Move => PlayerEvent::MouseMove { x, y },
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
                                            let Some((key_code, key_char)) =
                                                android_keycode_to_ruffle(event.key_code())
                                            else {
                                                return InputStatus::Unhandled;
                                            };
                                            let ruffle_event = match event.action() {
                                                KeyAction::Down => {
                                                    PlayerEvent::KeyDown { key_code, key_char }
                                                }
                                                KeyAction::Up => {
                                                    PlayerEvent::KeyUp { key_code, key_char }
                                                }
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
                    PollEvent::Wake => {} // A task tried to wake us, we'll recv it below
                    PollEvent::Timeout => {} // No events happened, we'll tick as normal below
                    _ => {}               // Unknown future event
                }
            },
        );

        match receiver.try_recv() {
            Err(_) => {}
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
                    JavaInterface::show_context_menu(&mut env, &activity, &items);
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

    unsafe {
        let vm = JavaVM::from_raw(app.vm_as_ptr() as *mut sys::JavaVM).expect("JVM must exist");
        let activity = JObject::from_raw(app.activity_as_ptr() as jobject);
        // Ensure that we take the EventSender back, or we'll leak it
        let _: Result<EventSender, _> = vm
            .get_env()
            .unwrap()
            .take_rust_field(activity, "eventLoopHandle");
    }
}

#[no_mangle]
#[allow(clippy::missing_safety_doc)]
pub unsafe extern "C" fn Java_rs_ruffle_PlayerActivity_keydown(
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
#[allow(clippy::missing_safety_doc)]
pub unsafe extern "C" fn Java_rs_ruffle_PlayerActivity_keyup(
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

pub fn get_jvm<'a>() -> Result<(jni::JavaVM, JObject<'a>), Box<dyn std::error::Error>> {
    // Create a VM for executing Java calls
    let context = ndk_context::android_context();
    let activity = unsafe { JObject::from_raw(context.context().cast()) };
    let vm = unsafe { jni::JavaVM::from_raw(context.vm().cast()) }?;

    Ok((vm, activity))
}

#[no_mangle]
#[allow(clippy::missing_safety_doc)]
pub unsafe extern "C" fn Java_rs_ruffle_PlayerActivity_requestContextMenu(
    mut env: JNIEnv,
    this: JObject,
) {
    let event_loop: MutexGuard<Sender<RuffleEvent>> =
        env.get_rust_field(this, "eventLoopHandle").unwrap();
    let _ = event_loop.send(RuffleEvent::RequestContextMenu);
}

#[no_mangle]
#[allow(clippy::missing_safety_doc)]
pub unsafe extern "C" fn Java_rs_ruffle_PlayerActivity_runContextMenuCallback(
    mut env: JNIEnv,
    this: JObject,
    index: jint,
) {
    let event_loop: MutexGuard<Sender<RuffleEvent>> =
        env.get_rust_field(this, "eventLoopHandle").unwrap();
    let _ = event_loop.send(RuffleEvent::RunContextMenuCallback(index as usize));
}

#[no_mangle]
#[allow(clippy::missing_safety_doc)]
pub unsafe extern "C" fn Java_rs_ruffle_PlayerActivity_clearContextMenu(
    mut env: JNIEnv,
    this: JObject,
) {
    let event_loop: MutexGuard<Sender<RuffleEvent>> =
        env.get_rust_field(this, "eventLoopHandle").unwrap();
    let _ = event_loop.send(RuffleEvent::ClearContextMenu);
}

#[no_mangle]
#[allow(clippy::missing_safety_doc)]
pub unsafe extern "C" fn Java_rs_ruffle_PlayerActivity_nativeInit(mut env: JNIEnv, class: JClass) {
    JavaInterface::init(&mut env, &class)
}

fn get_loc_on_screen() -> (i32, i32) {
    let (jvm, activity) = get_jvm().unwrap();
    let mut env = jvm.attach_current_thread().unwrap();

    // no worky :(
    //ndk_glue::native_activity().show_soft_input(true);

    JavaInterface::get_loc_on_screen(&mut env, &activity)
}

fn get_view_size() -> Result<(i32, i32), Box<dyn std::error::Error>> {
    let (jvm, activity) = get_jvm()?;
    let mut env = jvm.attach_current_thread()?;

    let width = JavaInterface::get_surface_width(&mut env, &activity);
    let height = JavaInterface::get_surface_height(&mut env, &activity);

    Ok((width, height))
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
