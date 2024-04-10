use jni::objects::{
    JByteArray, JClass, JIntArray, JMethodID, JObject, JString, JValue, ReleaseMode,
};
use jni::signature::{Primitive, ReturnType};
use jni::JNIEnv;
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
    pub fn get_surface_width(env: &mut JNIEnv, this: &JObject) -> i32 {
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

    pub fn get_surface_height(env: &mut JNIEnv, this: &JObject) -> i32 {
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

    pub fn show_context_menu(env: &mut JNIEnv, this: &JObject, items: &[ContextMenuItem]) {
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
            env.set_object_array_element(&arr, i as i32, s).unwrap();
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

    pub fn get_swf_bytes(env: &mut JNIEnv, this: &JObject) -> Option<Vec<u8>> {
        let result = unsafe {
            env.call_method_unchecked(this, Self::get().get_swf_bytes, ReturnType::Array, &[])
        };
        let object = result.expect("getSwfBytes() must never throw").l().unwrap();
        if object.is_null() {
            return None;
        }

        let arr = JByteArray::from(object);
        let elements = unsafe {
            env.get_array_elements(&arr, ReleaseMode::NoCopyBack)
                .unwrap()
        };
        let data =
            unsafe { std::slice::from_raw_parts(elements.as_ptr() as *mut u8, elements.len()) };
        Some(data.to_vec())
    }

    pub fn get_swf_uri(env: &mut JNIEnv, this: &JObject) -> String {
        let result = unsafe {
            env.call_method_unchecked(this, Self::get().get_swf_uri, ReturnType::Object, &[])
        };
        let object = result.expect("getSwfUri() must never throw").l().unwrap();
        if object.is_null() {
            return Default::default();
        }
        let string_object = JString::from(object);
        let java_string = unsafe { env.get_string_unchecked(&string_object) };
        let url = java_string.unwrap().to_string_lossy().to_string();
        url
    }

    pub fn get_trace_output(env: &mut JNIEnv, this: &JObject) -> Option<PathBuf> {
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
        let string_object = JString::from(object);
        let java_string = unsafe { env.get_string_unchecked(&string_object) };
        let url = java_string.unwrap().to_string_lossy().to_string();
        Some(url.into())
    }

    pub fn get_loc_in_window(env: &mut JNIEnv, this: &JObject) -> (i32, i32) {
        let result = unsafe {
            env.call_method_unchecked(this, Self::get().get_loc_in_window, ReturnType::Array, &[])
        };
        let object = result
            .expect("getLocInWindow() must never throw")
            .l()
            .unwrap();
        let arr = JIntArray::from(object);
        let elements = unsafe {
            env.get_array_elements(&arr, ReleaseMode::NoCopyBack)
                .unwrap()
        };
        let data = unsafe { std::slice::from_raw_parts(elements.as_ptr(), elements.len()) };
        (data[0], data[1])
    }

    pub fn get_android_data_storage_dir(env: &mut JNIEnv, this: &JObject) -> PathBuf {
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
        let string_object = JString::from(object);
        let java_string = unsafe { env.get_string_unchecked(&string_object) };
        let path = java_string.unwrap().to_string_lossy().to_string();
        PathBuf::from(path)
    }

    pub fn get() -> &'static JavaInterface {
        JAVA_INTERFACE
            .get()
            .expect("Java interface must have been created via nativeInit()")
    }

    pub fn init(env: &mut JNIEnv, class: &JClass) {
        JAVA_INTERFACE
            .set(JavaInterface {
                get_surface_width: env
                    .get_method_id(class, "getSurfaceWidth", "()I")
                    .expect("getSurfaceWidth must exist"),
                get_surface_height: env
                    .get_method_id(class, "getSurfaceHeight", "()I")
                    .expect("getSurfaceHeight must exist"),
                show_context_menu: env
                    .get_method_id(class, "showContextMenu", "([Ljava/lang/String;)V")
                    .expect("showContextMenu must exist"),
                get_swf_bytes: env
                    .get_method_id(class, "getSwfBytes", "()[B")
                    .expect("getSwfBytes must exist"),
                get_swf_uri: env
                    .get_method_id(class, "getSwfUri", "()Ljava/lang/String;")
                    .expect("getSwfUri must exist"),
                get_trace_output: env
                    .get_method_id(class, "getTraceOutput", "()Ljava/lang/String;")
                    .expect("getTraceOutput must exist"),
                get_loc_in_window: env
                    .get_method_id(class, "getLocInWindow", "()[I")
                    .expect("getLocInWindow must exist"),
                get_android_data_storage_dir: env
                    .get_method_id(class, "getAndroidDataStorageDir", "()Ljava/lang/String;")
                    .expect("getAndroidDataStorageDir must exist"),
            })
            .unwrap_or_else(|_| panic!("Init cannot be called more than once!"))
    }
}
