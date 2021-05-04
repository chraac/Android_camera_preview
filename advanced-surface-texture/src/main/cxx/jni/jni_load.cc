
#include <android/log.h>
#include <jni.h>

#include "com_chraac_advsurfacetexture_SurfaceExt.hh"

using namespace adv_surface_texture;

namespace {

constexpr const char *kLogTag = "JNI_LOAD";

} // namespace

/* Library load */
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void * /*reserved*/) {
  JNIEnv *env = nullptr;
  if (jvm->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag,
                        "GetEnv failed with error.");
    return JNI_EVERSION;
  }

  return JNISurfaceExt::GetInstance().Load(jvm, env) ? JNI_VERSION_1_6
                                                     : JNI_EVERSION;
}

/* Library unload */
extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *jvm,
                                               void * /*reserved*/) {
  __android_log_print(ANDROID_LOG_ERROR, kLogTag, "Unload JNI");

  JNIEnv *env = nullptr;
  if (jvm->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag,
                        "GetEnv failed with error.");
    return;
  }

  JNISurfaceExt::GetInstance().Unload(env);
}
