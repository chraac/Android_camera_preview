
#include "com_chraac_advsurfacetexture_SurfaceExt.hh"
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

namespace {
using namespace adv_surface_texture;
constexpr const char *kLogTag = "JNISurfaceExt";
constexpr const char *kClassName = "com/chraac/advsurfacetexture/SurfaceExt";

/*
 * Class:     com_chraac_advsurfacetexture_EGLFunctions
 * Method:    nativeSetBuffersGeometry
 * Signature:
 * (Landroid/view/Surface;III)Z
 */
jboolean JNICALL JniNativeSetBuffersGeometry(JNIEnv *env, jobject,
                                             jobject surface, jint width,
                                             jint height, jint format) {
  if (!surface) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag, "Invalid surface object.");
    return JNI_FALSE;
  }

  auto native_window = ANativeWindow_fromSurface(env, surface);
  if (!native_window) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag,
                        "Invalid native window object.");
    return JNI_FALSE;
  }

  return ANativeWindow_setBuffersGeometry(native_window, int32_t(width),
                                          int32_t(height), int32_t(format))
             ? JNI_FALSE
             : JNI_TRUE;
}

const JNINativeMethod g_methods[] = {{"nativeSetBuffersGeometry",
                                      "(Landroid/view/Surface;III)Z",
                                      (void *)JniNativeSetBuffersGeometry}};

} // namespace

namespace adv_surface_texture {

bool JNISurfaceExt::Load(JavaVM *jvm, JNIEnv *env) {
  UniquePtrJClass ext_clazz(
      env->FindClass(kClassName),
      GetJNIDestructorFunctor(jvm, &JNIEnv::DeleteLocalRef));
  if (!ext_clazz) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag, "FindClass failed.");
    return false;
  }

  if (env->RegisterNatives(ext_clazz.get(), g_methods,
                           sizeof(g_methods) / sizeof(g_methods[0])) < 0) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag, "RegisterNatives failed.");
    return false;
  }

  ext_clazz_ =
      UniquePtrJClass((jclass)env->NewGlobalRef(ext_clazz.get()),
                      GetJNIDestructorFunctor(jvm, &JNIEnv::DeleteGlobalRef));
  java_vm_ = jvm;
  return true;
}

void JNISurfaceExt::Unload(JNIEnv *) {
  ext_clazz_.reset();
  java_vm_ = nullptr;
}

} // namespace adv_surface_texture