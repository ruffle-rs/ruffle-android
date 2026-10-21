use jni::objects::{
    JByteArray, JClass, JIntArray, JMethodID, JObject, JObjectArray, JString, JValue, ReleaseMode,
};
use jni::signature::{Primitive, ReturnType};
use jni::{jni_sig, jni_str, Env};
use ruffle_core::{ContextMenuItem, RootTimeline};
use std::path::PathBuf;
use std::sync::OnceLock;

/// Handles to various items on the Java `PlayerActivity` class.
/// This is statically initialized once at startup, via the Java method `nativeInit()`.
/// This avoids needing to pay the lookup and validation penalty for every single call back into Java,
/// which can be a significant cost.
pub struct JavaInterface {
    get_surface_width: JMethodID,
    get_surface_height: JMethodID,
    show_context_menu: JMethodID,
    get_swf_bytes: JMethodID,
    get_swf_uri: JMethodID,
    get_trace_output: JMethodID,
    get_loc_in_window: JMethodID,
    get_android_data_storage_dir: JMethodID,
    get_folder_swf_path: JMethodID,
    open_folder_file: JMethodID,
}

static JAVA_INTERFACE: OnceLock<JavaInterface> = OnceLock::new();

impl JavaInterface {
    pub fn get_surface_width(env: &mut Env, this: &JObject) -> i32 {
        let result = unsafe {
            env.call_method_unchecked(
                this,
                Self::get().get_surface_width,
                ReturnType::Primitive(Primitive::Int),
                &[],
            )
        };
        result
            .expect("getSurfaceWidth() must never throw")
            .i()
            .unwrap()
    }

    pub fn get_surface_height(env: &mut Env, this: &JObject) -> i32 {
        let result = unsafe {
            env.call_method_unchecked(
                this,
                Self::get().get_surface_height,
                ReturnType::Primitive(Primitive::Int),
                &[],
            )
        };
        result
            .expect("getSurfaceHeight() must never throw")
            .i()
            .unwrap()
    }

    /// Shows the context menu, along with the scenes and frame labels of the root timeline
    /// (when there is one), and the game's own scene list (when it has one), so that the
    /// user can jump to any of them.
    pub fn show_context_menu(
        env: &mut Env,
        this: &JObject,
        items: &[ContextMenuItem],
        timeline: Option<&RootTimeline>,
    ) {
        let arr = JObjectArray::<JString>::new(env, items.len(), JString::null()).unwrap();
        for (i, e) in items.iter().enumerate() {
            let s = env
                .new_string(format!(
                    "{} {} {} {}",
                    e.enabled, e.separator_before, e.checked, e.caption
                ))
                .unwrap();
            arr.set_element(env, i, &s).unwrap();
        }

        // Each entry is "<scene|label|game> <enabled> <checked> <frame> <caption>", where
        // the frame of a game scene is its index in the game's scene list, and the caption
        // comes last because it may contain spaces.
        let mut entries = vec![];
        if let Some(timeline) = timeline {
            if let Some(game_scenes) = &timeline.game_scenes {
                for (index, name) in game_scenes.names.iter().enumerate() {
                    let number = index + 1;
                    let caption = if name.is_empty() {
                        number.to_string()
                    } else {
                        format!("{number}. {name}")
                    };
                    entries.push(format!(
                        "game true {} {index} {caption}",
                        game_scenes.current == Some(index)
                    ));
                }
            }
            let current_scene = timeline.current_scene();
            for (index, scene) in timeline.scenes.iter().enumerate() {
                entries.push(format!(
                    "scene {} {} {} {} ({}–{})",
                    timeline.is_frame_loaded(scene.start),
                    current_scene == Some(index),
                    scene.start,
                    scene.name,
                    scene.start,
                    scene.end()
                ));
            }
            for label in &timeline.labels {
                entries.push(format!(
                    "label {} false {} {} ({})",
                    timeline.is_frame_loaded(label.frame),
                    label.frame,
                    label.name,
                    label.frame
                ));
            }
        }
        let timeline_arr =
            JObjectArray::<JString>::new(env, entries.len(), JString::null()).unwrap();
        for (i, e) in entries.iter().enumerate() {
            let s = env.new_string(e).unwrap();
            timeline_arr.set_element(env, i, &s).unwrap();
        }
        let (current_frame, frames_loaded, total_frames) = timeline
            .map(|t| (t.current_frame, t.frames_loaded, t.total_frames))
            .unwrap_or_default();

        let result = unsafe {
            env.call_method_unchecked(
                this,
                Self::get().show_context_menu,
                ReturnType::Primitive(Primitive::Void),
                &[
                    JValue::Object(arr.as_ref()).as_jni(),
                    JValue::Object(timeline_arr.as_ref()).as_jni(),
                    JValue::Int(current_frame.into()).as_jni(),
                    JValue::Int(frames_loaded.into()).as_jni(),
                    JValue::Int(total_frames.into()).as_jni(),
                ],
            )
        };
        result.expect("showContextMenu() must never throw");
    }

    pub fn get_swf_bytes(env: &mut Env, this: &JObject) -> Option<Vec<u8>> {
        let result = unsafe {
            env.call_method_unchecked(this, Self::get().get_swf_bytes, ReturnType::Array, &[])
        };
        let object = result.expect("getSwfBytes() must never throw").l().unwrap();
        if object.is_null() {
            return None;
        }

        let arr = unsafe { JByteArray::from_raw(env, object.into_raw()) };
        let elements = unsafe { arr.get_elements(env, ReleaseMode::NoCopyBack).unwrap() };
        let data =
            unsafe { std::slice::from_raw_parts(elements.as_ptr() as *mut u8, elements.len()) };
        Some(data.to_vec())
    }

    pub fn get_swf_uri(env: &mut Env, this: &JObject) -> String {
        let result = unsafe {
            env.call_method_unchecked(this, Self::get().get_swf_uri, ReturnType::Object, &[])
        };
        let object = result.expect("getSwfUri() must never throw").l().unwrap();
        if object.is_null() {
            return Default::default();
        }
        let string_object = unsafe { env.as_cast_unchecked::<JString>(&object) };
        string_object.try_to_string(env).unwrap()
    }

    pub fn get_trace_output(env: &mut Env, this: &JObject) -> Option<PathBuf> {
        let result = unsafe {
            env.call_method_unchecked(this, Self::get().get_trace_output, ReturnType::Object, &[])
        };
        let object = result
            .expect("getTraceOutput() must never throw")
            .l()
            .unwrap();
        if object.is_null() {
            return None;
        }
        let string_object = unsafe { env.as_cast_unchecked::<JString>(&object) };
        let url = string_object.try_to_string(env).unwrap();
        Some(url.into())
    }

    pub fn get_loc_in_window(env: &mut Env, this: &JObject) -> (i32, i32) {
        let result = unsafe {
            env.call_method_unchecked(this, Self::get().get_loc_in_window, ReturnType::Array, &[])
        };
        let object = result
            .expect("getLocInWindow() must never throw")
            .l()
            .unwrap();
        let arr = unsafe { JIntArray::from_raw(env, object.into_raw()) };
        let elements = unsafe { arr.get_elements(env, ReleaseMode::NoCopyBack).unwrap() };
        let data = unsafe { std::slice::from_raw_parts(elements.as_ptr(), elements.len()) };
        (data[0], data[1])
    }

    pub fn get_android_data_storage_dir(env: &mut Env, this: &JObject) -> PathBuf {
        let result = unsafe {
            env.call_method_unchecked(
                this,
                Self::get().get_android_data_storage_dir,
                ReturnType::Object,
                &[],
            )
        };
        let object = result
            .expect("getAndroidDataStorageDir() must never throw")
            .l()
            .unwrap();
        let string_object = unsafe { env.as_cast_unchecked::<JString>(&object) };
        let path = string_object.try_to_string(env).unwrap();
        PathBuf::from(path)
    }

    /// The path of the movie within the folder it was opened from,
    /// or `None` if it wasn't opened from a folder.
    pub fn get_folder_swf_path(env: &mut Env, this: &JObject) -> Option<String> {
        let result = unsafe {
            env.call_method_unchecked(
                this,
                Self::get().get_folder_swf_path,
                ReturnType::Object,
                &[],
            )
        };
        let object = result
            .expect("getFolderSwfPath() must never throw")
            .l()
            .unwrap();
        if object.is_null() {
            return None;
        }
        let string_object = unsafe { env.as_cast_unchecked::<JString>(&object) };
        Some(string_object.try_to_string(env).unwrap())
    }

    /// Opens the file at `path` within the folder the movie was opened from,
    /// returning a file descriptor that the caller owns, or a negative number on failure.
    pub fn open_folder_file(env: &mut Env, this: &JObject, path: &str) -> i32 {
        let path = env.new_string(path).unwrap();
        let result = unsafe {
            env.call_method_unchecked(
                this,
                Self::get().open_folder_file,
                ReturnType::Primitive(Primitive::Int),
                &[JValue::Object(path.as_ref()).as_jni()],
            )
        };
        result
            .expect("openFolderFile() must never throw")
            .i()
            .unwrap()
    }

    pub fn get() -> &'static JavaInterface {
        JAVA_INTERFACE
            .get()
            .expect("Java interface must have been created via nativeInit()")
    }

    pub fn init(env: &mut Env, class: &JClass) {
        let _ = JAVA_INTERFACE.set(JavaInterface {
            get_surface_width: env
                .get_method_id(class, jni_str!("getSurfaceWidth"), jni_sig!("()I"))
                .expect("getSurfaceWidth must exist"),
            get_surface_height: env
                .get_method_id(class, jni_str!("getSurfaceHeight"), jni_sig!("()I"))
                .expect("getSurfaceHeight must exist"),
            show_context_menu: env
                .get_method_id(
                    class,
                    jni_str!("showContextMenu"),
                    jni_sig!("([Ljava/lang/String;[Ljava/lang/String;III)V"),
                )
                .expect("showContextMenu must exist"),
            get_swf_bytes: env
                .get_method_id(class, jni_str!("getSwfBytes"), jni_sig!("()[B"))
                .expect("getSwfBytes must exist"),
            get_swf_uri: env
                .get_method_id(
                    class,
                    jni_str!("getSwfUri"),
                    jni_sig!("()Ljava/lang/String;"),
                )
                .expect("getSwfUri must exist"),
            get_trace_output: env
                .get_method_id(
                    class,
                    jni_str!("getTraceOutput"),
                    jni_sig!("()Ljava/lang/String;"),
                )
                .expect("getTraceOutput must exist"),
            get_loc_in_window: env
                .get_method_id(class, jni_str!("getLocInWindow"), jni_sig!("()[I"))
                .expect("getLocInWindow must exist"),
            get_android_data_storage_dir: env
                .get_method_id(
                    class,
                    jni_str!("getAndroidDataStorageDir"),
                    jni_sig!("()Ljava/lang/String;"),
                )
                .expect("getAndroidDataStorageDir must exist"),
            get_folder_swf_path: env
                .get_method_id(
                    class,
                    jni_str!("getFolderSwfPath"),
                    jni_sig!("()Ljava/lang/String;"),
                )
                .expect("getFolderSwfPath must exist"),
            open_folder_file: env
                .get_method_id(
                    class,
                    jni_str!("openFolderFile"),
                    jni_sig!("(Ljava/lang/String;)I"),
                )
                .expect("openFolderFile must exist"),
        });
    }
}
