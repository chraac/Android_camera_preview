
#include "com_chraac_advsurfacetexture_NativeObject.hh"
#include <android/log.h>

namespace {

constexpr const char *kLogTag = "JNINativeObject";
constexpr const char *kNativeObjectClassName =
    "com/chraac/advsurfacetexture/NativeObject";

} // namespace

namespace adv_surface_texture {

bool JNINativeObject::Load(JavaVM *jvm, JNIEnv *env) {
  UniquePtrJClass native_object_class(
      env->FindClass(kNativeObjectClassName),
      GetJNIDestructorFunctor(jvm, &JNIEnv::DeleteLocalRef));
  if (!native_object_class) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag, "FindClass failed.");
    return false;
  }

  native_pointer_getter_ =
      env->GetMethodID(native_object_class.get(), "getNative", "()J");
  native_pointer_setter_ =
      env->GetMethodID(native_object_class.get(), "setNative", "(J)V");
  native_object_clazz_ =
      UniquePtrJClass((jclass)env->NewGlobalRef(native_object_class.get()),
                      GetJNIDestructorFunctor(jvm, &JNIEnv::DeleteGlobalRef));
  return true;
}

void JNINativeObject::Unload(JNIEnv *) {
  native_pointer_getter_ = nullptr;
  native_pointer_setter_ = nullptr;
  native_object_clazz_.reset();
}

void JNINativeObject::SetNativeField(JNIEnv *env, jobject object, void *ptr) {
  if (IsValid()) {
    env->CallVoidMethod(object, native_pointer_setter_,
                        reinterpret_cast<jlong>(ptr));
  }
}

void *JNINativeObject::GetNativeFieldInternal(JNIEnv *env,
                                              jobject object) const {
  return IsValid() && object ? reinterpret_cast<void *>(env->CallLongMethod(
                                   object, native_pointer_getter_))
                             : nullptr;
}

} // namespace adv_surface_texture
