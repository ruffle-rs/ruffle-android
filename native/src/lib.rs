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
use std::sync::MutexGuard;
use std::{
    sync::{Arc, Mutex},
    time::Instant,
};
use wgpu::rwh::{HasDisplayHandle, HasWindowHandle};

use android_activity::AndroidApp;
use jni::objects::JValue;
use winit::{
    event::{DeviceEvent, ElementState, Event, TouchPhase, WindowEvent},
    event_loop::{ControlFlow, EventLoop, EventLoopBuilder},
    platform::android::EventLoopBuilderExtAndroid,
    window::Window,
};

use audio::AAudioAudioBackend;
use keycodes::winit_to_ruffle_key_code;
use navigator::ExternalNavigatorBackend;
use ruffle_core::backend::storage::MemoryStorageBackend;
use url::Url;

use executor::WinitAsyncExecutor;

use ruffle_core::{
    events::{KeyCode, MouseButton as RuffleMouseButton, PlayerEvent},
    tag_utils::SwfMovie,
    Player, PlayerBuilder, ViewportDimensions,
};

use ruffle_render_wgpu::{backend::WgpuRenderBackend, target::SwapChainTarget};
use winit::event_loop::EventLoopProxy;

/// Represents a current Player and any associated state with that player,
/// which may be lost when this Player is closed (dropped)
struct ActivePlayer {
    player: Arc<Mutex<Player>>,
    executor: Arc<Mutex<WinitAsyncExecutor>>,
}

fn run(event_loop: EventLoop<custom_event::RuffleEvent>, window: Window) {
    let mut playerbox: Option<ActivePlayer> = None;
    let mut time = Instant::now();
    let mut next_frame_time = Instant::now();

    log::info!("Starting event loop...");

    let event_loop_proxy = event_loop.create_proxy();

    let _ = event_loop.run(move |event, elwt| {
        elwt.set_control_flow(ControlFlow::Poll);

        match event {
            Event::WindowEvent {
                event: WindowEvent::CloseRequested,
                ..
            } => elwt.exit(),

            Event::WindowEvent { event, .. } => match event {
                WindowEvent::Resized(size) => {
                    let player = unsafe { &playerbox.as_ref().unwrap().player };
                    let mut player_lock = player.lock().unwrap();

                    let viewport_scale_factor = window.scale_factor();

                    player_lock.set_viewport_dimensions(ViewportDimensions {
                        width: size.width,
                        height: size.height,
                        scale_factor: viewport_scale_factor,
                    });

                    player_lock
                        .renderer_mut()
                        .set_viewport_dimensions(ViewportDimensions {
                            width: size.width,
                            height: size.height,
                            scale_factor: viewport_scale_factor,
                        });

                    window.request_redraw();
                }

                // Render
                WindowEvent::RedrawRequested => {
                    // TODO: Don't render when minimized to avoid potential swap chain errors in `wgpu`.
                    // TODO: also disable when suspended!

                    if unsafe { playerbox.is_some() } {
                        let player = unsafe { &playerbox.as_ref().unwrap().player };

                        let mut player_lock = player.lock().unwrap();
                        if player_lock.is_playing() {
                            log::info!("playing, rendering");
                            player_lock.render();
                        } else {
                            log::info!("not playing, not rendering");
                        }
                    }
                }

                WindowEvent::Touch(touch) => {
                    log::info!("touch: {:?}", touch);
                    let player = unsafe { &playerbox.as_ref().unwrap().player };

                    let mut player_lock = player.lock().unwrap();

                    let coords: (i32, i32) = get_loc_on_screen().unwrap();

                    let mut x = touch.location.x - coords.0 as f64;
                    let mut y = touch.location.y - coords.1 as f64;

                    let view_size = get_view_size().unwrap();

                    x = x * window.inner_size().width as f64 / view_size.0 as f64;
                    y = y * window.inner_size().height as f64 / view_size.1 as f64;

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

                WindowEvent::KeyboardInput {
                    event: key_event, ..
                } => {
                    let player = unsafe { &playerbox.as_ref().unwrap().player };

                    log::info!("keyboard event: {:?}", key_event);

                    let mut player_lock = player.lock().unwrap();

                    let key_code = winit_to_ruffle_key_code(&key_event);
                    let key_char = key_event.text.clone().and_then(|text| text.chars().last());
                    let event = match key_event.state {
                        ElementState::Pressed => PlayerEvent::KeyDown { key_code, key_char },
                        ElementState::Released => PlayerEvent::KeyUp { key_code, key_char },
                    };
                    log::warn!("Ruffle key event: {:?}", event);
                    player_lock.handle_event(event);

                    key_event.text.unwrap_or_default().chars().for_each(|c| {
                        let event = PlayerEvent::TextInput { codepoint: c };
                        log::warn!("Ruffle text input event: {:?}", event);
                        player_lock.handle_event(event);
                    });

                    if player_lock.needs_render() {
                        window.request_redraw();
                    }
                }
                _ => {}
            },

            Event::DeviceEvent { event, .. } => {
                log::info!("device event: {:?}", event);
                match event {
                    DeviceEvent::Key(key) => {
                        log::info!("key: {:?}", key);
                    }
                    _ => {}
                }
            }
            Event::Resumed => {
                log::info!("RUFFLE RESUMED");

                if unsafe { playerbox.is_none() } {
                    log::info!("playerbox is none");
                    //let size = window.inner_size();

                    let renderer = unsafe {
                        // TODO: make this take an Arc<Window> instead?
                        WgpuRenderBackend::for_window_unsafe(
                            wgpu::SurfaceTargetUnsafe::RawHandle {
                                raw_display_handle: window.display_handle().unwrap().into(),
                                raw_window_handle: window.window_handle().unwrap().into(),
                            },
                            (window.inner_size().width, window.inner_size().height),
                            wgpu::Backends::GL,
                            wgpu::PowerPreference::HighPerformance,
                            None,
                        )
                            .unwrap()
                    };
                    let movie_url = Url::parse("file://movie.swf").unwrap();

                    let (executor, channel) = WinitAsyncExecutor::new(event_loop_proxy.clone());
                    let navigator = ExternalNavigatorBackend::new(
                        movie_url.clone(),
                        channel,
                        event_loop_proxy.clone(),
                        true,
                        ruffle_core::backend::navigator::OpenURLMode::Allow,
                    );

                    unsafe {
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
                        })
                    };

                    let player = unsafe { &playerbox.as_ref().unwrap().player };
                    let mut player_lock = player.lock().unwrap();

                    match get_swf_bytes() {
                        Ok(bytes) => {
                            let movie =
                                SwfMovie::from_data(&bytes, "file://movie.swf".to_string(), None)
                                    .unwrap();

                            player_lock.mutate_with_update_context(|context| {
                                context.set_root_movie(movie);
                            });
                            player_lock.set_is_playing(true); // Desktop player will auto-play.

                            let viewport_size = window.inner_size();
                            let viewport_scale_factor = window.scale_factor();
                            player_lock.set_letterbox(ruffle_core::config::Letterbox::On);

                            log::info!("VIEWP SIZE: {:?}", viewport_size);

                            player_lock.set_viewport_dimensions(ViewportDimensions {
                                width: viewport_size.width,
                                height: viewport_size.height,
                                scale_factor: viewport_scale_factor,
                            });

                            player_lock.renderer_mut().set_viewport_dimensions(
                                ViewportDimensions {
                                    width: viewport_size.width,
                                    height: viewport_size.height,
                                    scale_factor: viewport_scale_factor,
                                },
                            );

                            time = Instant::now();
                            next_frame_time = Instant::now();

                            log::info!("MOVIE STARTED");
                        }
                        Err(e) => {
                            log::error!("{}", e);
                        }
                    }
                } else {
                    log::info!("playerbox is some, playing");

                    let player = unsafe { &playerbox.as_ref().unwrap().player };
                    let mut player_lock = player.lock().unwrap();

                    unsafe {
                        player_lock
                            .renderer_mut()
                            .downcast_mut::<WgpuRenderBackend<SwapChainTarget>>()
                            .unwrap()
                            // TODO: make this take an Arc<Window> instead?
                            .recreate_surface_unsafe(
                                wgpu::SurfaceTargetUnsafe::RawHandle {
                                    raw_display_handle: window.display_handle().unwrap().into(),
                                    raw_window_handle: window.window_handle().unwrap().into(),
                                },
                                (window.inner_size().width, window.inner_size().height),
                            );
                    }
                    player_lock.set_is_playing(true);
                }
            }
            Event::Suspended => {
                log::info!("RUFFLE SUSPENDED");

                let player = unsafe { &playerbox.as_ref().unwrap().player };
                let mut player_lock = player.lock().unwrap();

                player_lock.set_is_playing(false);
            }
            Event::AboutToWait => {
                let new_time = Instant::now();
                let dt = new_time.duration_since(time).as_micros();

                if dt > 0 {
                    time = new_time;
                    if unsafe { playerbox.is_some() } {
                        let player = unsafe { &playerbox.as_ref().unwrap().player };

                        let mut player_lock = player.lock().unwrap();
                        player_lock.tick(dt as f64 / 1000.0);
                        let audio = player_lock
                            .audio_mut()
                            .downcast_mut::<AAudioAudioBackend>()
                            .unwrap();

                        audio.recreate_stream_if_needed();

                        next_frame_time = new_time + player_lock.time_til_next_frame();

                        if player_lock.needs_render() {
                            window.request_redraw();
                        }
                    }
                }
            }

            winit::event::Event::UserEvent(RuffleEvent::TaskPoll) => {
                if unsafe { playerbox.is_some() } {
                    let executor = unsafe { &playerbox.as_ref().unwrap().executor };
                    executor
                        .lock()
                        .expect("Executor lock must be available")
                        .poll_all()
                }
            }

            Event::UserEvent(RuffleEvent::VirtualKeyEvent {
                                 down,
                                 key_code,
                                 key_char,
                             }) => {
                if unsafe { playerbox.is_some() } {
                    let player = unsafe { &playerbox.as_ref().unwrap().player };
                    let mut player_lock = player.lock().unwrap();

                    log::warn!("keydown!");

                    let event = if down {
                        PlayerEvent::KeyDown { key_code, key_char }
                    } else {
                        PlayerEvent::KeyUp { key_code, key_char }
                    };
                    log::warn!("{:#?}", event);
                    player_lock.handle_event(event);

                    if down {
                        // NOTE: this is a HACK
                        if let Some(key) = key_char {
                            let event = PlayerEvent::TextInput { codepoint: key };
                            log::info!("faking text input: {:?}", key);
                            player_lock.handle_event(event);
                        }
                    }
                }
            }

            Event::UserEvent(RuffleEvent::RunContextMenuCallback(index)) => {
                if let Some(player) = unsafe { playerbox.as_ref() } {
                    if let Ok(mut player_lock) = player.player.lock() {
                        player_lock.run_context_menu_callback(index);
                    }
                }
            }

            Event::UserEvent(RuffleEvent::ClearContextMenu) => {
                if let Some(player) = unsafe { playerbox.as_ref() } {
                    if let Ok(mut player_lock) = player.player.lock() {
                        player_lock.clear_custom_menu_items();
                    }
                }
            }

            Event::UserEvent(RuffleEvent::Resize(viewport_dimensions)) => {
                if let Some(player) = unsafe { playerbox.as_ref() } {
                    if let Ok(mut player_lock) = player.player.lock() {
                        player_lock.set_viewport_dimensions(viewport_dimensions);
                    }
                }
            }

            Event::UserEvent(RuffleEvent::RequestContextMenu) => {
                if let Some(player) = unsafe { playerbox.as_ref() } {
                    if let Ok(mut player_lock) = player.player.lock() {
                        log::warn!("preparing context menu!");

                        let items = player_lock.prepare_context_menu();
                        let (jvm, activity) = get_jvm().unwrap();
                        let mut env = jvm.attach_current_thread().unwrap();
                        let arr = env
                            .new_object_array(
                                items.len() as i32,
                                "java/lang/String",
                                JObject::null(),
                            )
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

                        env.call_method(
                            activity,
                            "showContextMenu",
                            "([Ljava/lang/String;)V",
                            &[JValue::Object(&arr)],
                        );
                    }
                }
            }

            _ => {}
        }

        elwt.set_control_flow(ControlFlow::WaitUntil(next_frame_time));
    });
    log::info!("RUFFLE ENDED");
}

#[no_mangle]
pub unsafe extern "C" fn Java_rs_ruffle_FullscreenNativeActivity_keydown(
    mut env: JNIEnv,
    this: JObject,
    key_code_raw: jbyte,
    key_char_raw: jchar,
) {
    let event_loop: MutexGuard<EventLoopProxy<RuffleEvent>> =
        env.get_rust_field(this, "eventLoopHandle").unwrap();
    let key_code: KeyCode = ::std::mem::transmute(key_code_raw);
    let key_char = std::char::from_u32(key_char_raw as u32);
    let _ = event_loop.send_event(RuffleEvent::VirtualKeyEvent {
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
    let event_loop: MutexGuard<EventLoopProxy<RuffleEvent>> =
        env.get_rust_field(this, "eventLoopHandle").unwrap();
    let key_code: KeyCode = ::std::mem::transmute(key_code_raw);
    let key_char = std::char::from_u32(key_char_raw as u32);
    let _ = event_loop.send_event(RuffleEvent::VirtualKeyEvent {
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
    let event_loop: MutexGuard<EventLoopProxy<RuffleEvent>> =
        env.get_rust_field(this, "eventLoopHandle").unwrap();
    let size = get_view_size();
    if let Ok((w, h)) = size {
        let viewport_scale_factor = 1.0; //window.scale_factor();
        let _ = event_loop.send_event(RuffleEvent::Resize(ViewportDimensions {
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
    let event_loop: MutexGuard<EventLoopProxy<RuffleEvent>> =
        env.get_rust_field(this, "eventLoopHandle").unwrap();
    let _ = event_loop.send_event(RuffleEvent::RequestContextMenu);
}

#[no_mangle]
pub unsafe extern "C" fn Java_rs_ruffle_FullscreenNativeActivity_runContextMenuCallback(
    mut env: JNIEnv,
    this: JObject,
    index: jint,
) {
    let event_loop: MutexGuard<EventLoopProxy<RuffleEvent>> =
        env.get_rust_field(this, "eventLoopHandle").unwrap();
    let _ = event_loop.send_event(RuffleEvent::RunContextMenuCallback(index as usize));
}

#[no_mangle]
pub unsafe extern "C" fn Java_rs_ruffle_FullscreenNativeActivity_clearContextMenu(
    mut env: JNIEnv,
    this: JObject,
) {
    let event_loop: MutexGuard<EventLoopProxy<RuffleEvent>> =
        env.get_rust_field(this, "eventLoopHandle").unwrap();
    let _ = event_loop.send_event(RuffleEvent::ClearContextMenu);
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

    let vm =
        unsafe { JavaVM::from_raw(app.vm_as_ptr() as *mut sys::JavaVM).expect("JVM must exist") };
    let activity = unsafe { JObject::from_raw(app.activity_as_ptr() as jobject) };
    let event_loop = EventLoopBuilder::with_user_event()
        .with_android_app(app)
        .build()
        .expect("Failed to create event loop");
    let window = Window::new(&event_loop).unwrap();

    unsafe {
        vm.get_env().unwrap().set_rust_field(
            activity,
            "eventLoopHandle",
            event_loop.create_proxy(),
        );
    }

    run(event_loop, window);
}
