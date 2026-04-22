use jni::objects::{
    JByteArray, JClass, JIntArray, JMethodID, JObject, JObjectArray, JString, JValue, ReleaseMode,
};
use jni::signature::{Primitive, ReturnType};
use jni::{jni_sig, jni_str, Env};
use ruffle_core::ContextMenuItem;
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

    pub fn show_context_menu(env: &mut Env, this: &JObject, items: &[ContextMenuItem]) {
        let arr = JObjectArray::<JString>::new(env, items.len(), JString::null()).unwrap();
        for (i, e) in items.iter().enumerate() {
            let s = env
                .new_string(format!(
                    "{} {} {} {}",
                    e.enabled, e.separator_before, e.checked, e.caption
                ))
                .unwrap();
            arr.set_element(env, i, s).unwrap();
        }
        let result = unsafe {
            env.call_method_unchecked(
                this,
                Self::get().show_context_menu,
                ReturnType::Primitive(Primitive::Void),
                &[JValue::Object(&arr).as_jni()],
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
        let arr = env.cast_local::<JByteArray>(object).unwrap();
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
        let string_object = JString::cast_local(env, object).unwrap();
        string_object.to_string()
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
        let string_object = JString::cast_local(env, object).unwrap();

        Some(PathBuf::from(string_object.to_string()))
    }

    pub fn get_loc_in_window(env: &mut Env, this: &JObject) -> (i32, i32) {
        let result: Result<jni::JValueOwned<'_>, jni::errors::Error> = unsafe {
            env.call_method_unchecked(this, Self::get().get_loc_in_window, ReturnType::Array, &[])
        };
        let object = result
            .expect("getLocInWindow() must never throw")
            .l()
            .unwrap();
        let arr = env.cast_local::<JIntArray>(object).unwrap();
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
        let string_object = JString::cast_local(env, object).unwrap();

        PathBuf::from(string_object.to_string())
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
                    jni_sig!("([Ljava/lang/String;)V"),
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
        });
    }
}
