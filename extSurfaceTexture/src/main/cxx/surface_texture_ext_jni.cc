
#include "surface_texture_ext_jni.hh"
#include <android/hardware_buffer_jni.h>
#include <android/log.h>

namespace {

using namespace hardware_buffer_ext;

constexpr const char *kLogTag = "SurfaceTextureExtJNI";
constexpr const char *kEGLFunctionsClassName =
    "com/chraac/extsurfacetexture/EGLFunctionsImpl";
constexpr const char *kEGLImageClassName =
    "com/chraac/extsurfacetexture/EGLImageKHR";
constexpr const char *kEGLObjectHandleClassName =
    "android/opengl/EGLObjectHandle";

/*
 * Class:     com_chraac_extsurfacetexture_EGLFunctions
 * Method:    nativeCreateImageFromHardwareBuffer
 * Signature:
 *   (JLandroid/opengl/EGLDisplay;Landroid/hardware/HardwareBuffer;)Lcom/chraac/extsurfacetexture/EGLImage;
 */
jobject JNICALL JniNativeCreateImageFromHardwareBuffer(
    JNIEnv *env, jobject, jlong native, jobject display,
    jobject hardware_buffer) {
  AHardwareBuffer *ahardware_buffer =
      AHardwareBuffer_fromHardwareBuffer(env, hardware_buffer);
  if (!native || !ahardware_buffer) {
    return nullptr;
  }

  auto &inst = SurfaceTextureExtJNI::GetInstance();
  auto *functions = reinterpret_cast<HardwareBufferFunctions *>(native);
  auto native_buffer =
      functions->SoftLinkGetNativeClientBufferANDROID(ahardware_buffer);
  EGLDisplay egl_display = inst.GetEGLHandlerFormEGLObjectHandle(env, display);
  if (egl_display == EGL_NO_DISPLAY) {
    egl_display = eglGetCurrentDisplay();
  }

  constexpr const EGLint s_attribs[] = {EGL_IMAGE_PRESERVED_KHR, EGL_FALSE,
                                        EGL_NONE};
  auto image = functions->SoftLinkCreateImageKHR(egl_display, EGL_NO_CONTEXT,
                                                 EGL_NATIVE_BUFFER_ANDROID,
                                                 native_buffer, s_attribs);
  if (!image) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag,
                        "eglCreateImageKHR failed.");
    return nullptr;
  }

  return inst.CreateEGLImageFormEGLImageKHR(env, image);
}

/*
 * Class:     com_chraac_extsurfacetexture_EGLFunctions
 * Method:    nativeDestroyImageKHR
 * Signature:
 * (JLandroid/opengl/EGLDisplay;Lcom/chraac/extsurfacetexture/EGLImage;)V
 */
void JNICALL JniNativeDestroyImageKHR(JNIEnv *env, jobject, jlong native,
                                      jobject display, jobject image) {
  if (!native || !image) {
    return;
  }

  auto &inst = SurfaceTextureExtJNI::GetInstance();
  auto *functions = reinterpret_cast<HardwareBufferFunctions *>(native);
  EGLDisplay egl_display = inst.GetEGLHandlerFormEGLObjectHandle(env, display);
  if (egl_display == EGL_NO_DISPLAY) {
    egl_display = eglGetCurrentDisplay();
  }

  EGLImageKHR egl_image = inst.GetEGLHandlerFormEGLObjectHandle(env, image);
  functions->SoftLinkDestroyImageKHR(egl_display, egl_image);
}

/*
 * Class:     com_chraac_extsurfacetexture_EGLFunctions
 * Method:    nativeEGLImageTargetTexture2DOES
 * Signature: (JILcom/chraac/extsurfacetexture/EGLImage;)V
 */
void JNICALL JniNativeEGLImageTargetTexture2DOES(JNIEnv *env, jobject,
                                                 jlong native, jint target,
                                                 jobject image) {
  if (!native) {
    return;
  }

  auto &inst = SurfaceTextureExtJNI::GetInstance();
  auto *functions = reinterpret_cast<HardwareBufferFunctions *>(native);
  EGLImageKHR egl_image =
      image ? inst.GetEGLHandlerFormEGLObjectHandle(env, image)
            : EGL_NO_IMAGE_KHR;
  functions->SoftLinkEGLImageTargetTexture2DOES(target, egl_image);
}

const JNINativeMethod g_methods[] = {
    {"nativeCreateImageFromHardwareBuffer",
     "(JLandroid/opengl/EGLDisplay;Landroid/hardware/HardwareBuffer;)"
     "Lcom/chraac/extsurfacetexture/EGLImageKHR;",
     (void *)JniNativeCreateImageFromHardwareBuffer},
    {"nativeDestroyImageKHR",
     "(JLandroid/opengl/EGLDisplay;Lcom/chraac/extsurfacetexture/"
     "EGLImageKHR;)V",
     (void *)JniNativeDestroyImageKHR},
    {"nativeEGLImageTargetTexture2DOES",
     "(JILcom/chraac/extsurfacetexture/EGLImageKHR;)V",
     (void *)JniNativeEGLImageTargetTexture2DOES},
};

template <typename TyParam>
SurfaceTextureExtJNI::UniquePtrJClass::deleter_type
GetJNIDestructorFunctor(JavaVM *jvm, void (JNIEnv::*deleter)(TyParam)) {
  return [jvm, deleter](TyParam clazz) {
    JNIEnv *env = nullptr;
    if (jvm->GetEnv((void **)&env, JNI_VERSION_1_6) == JNI_OK) {
      (*env.*deleter)(clazz);
    }
  };
}

} // namespace

namespace hardware_buffer_ext {

bool SurfaceTextureExtJNI::Load(JavaVM *jvm, JNIEnv *env) {
  UniquePtrJClass functions_class(
      env->FindClass(kEGLFunctionsClassName),
      GetJNIDestructorFunctor(jvm, &JNIEnv::DeleteLocalRef));
  UniquePtrJClass image_clazz(
      env->FindClass(kEGLImageClassName),
      GetJNIDestructorFunctor(jvm, &JNIEnv::DeleteLocalRef));
  UniquePtrJClass egl_handler_clazz(
      env->FindClass(kEGLObjectHandleClassName),
      GetJNIDestructorFunctor(jvm, &JNIEnv::DeleteLocalRef));
  if (!functions_class || !image_clazz) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag, "FindClass failed.");
    return false;
  }

  if (env->RegisterNatives(functions_class.get(), g_methods,
                           sizeof(g_methods) / sizeof(g_methods[0])) < 0) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag, "RegisterNatives failed.");
    return false;
  }

  hardware_buffer_functions_ = std::make_unique<HardwareBufferFunctions>();
  if (!hardware_buffer_functions_->IsValid()) {
    hardware_buffer_functions_.reset();
    __android_log_print(ANDROID_LOG_ERROR, kLogTag,
                        "LoadFunctions failed with error.");
    return false;
  }

  functions_handler_ =
      env->GetStaticFieldID(functions_class.get(), "native", "J");
  env->SetStaticLongField(functions_class.get(), functions_handler_,
                          jlong(hardware_buffer_functions_.get()));

  egl_image_constructor_ =
      env->GetMethodID(image_clazz.get(), "<init>", "(J)V");
  egl_handler_get_native_handler_ =
      env->GetMethodID(egl_handler_clazz.get(), "getNativeHandle", "()J");

  java_vm_ = jvm;
  functions_clazz_ =
      UniquePtrJClass((jclass)env->NewGlobalRef(functions_class.get()),
                      GetJNIDestructorFunctor(jvm, &JNIEnv::DeleteGlobalRef));
  egl_image_clazz_ =
      UniquePtrJClass((jclass)env->NewGlobalRef(image_clazz.get()),
                      GetJNIDestructorFunctor(jvm, &JNIEnv::DeleteGlobalRef));
  egl_handler_clazz_ =
      UniquePtrJClass((jclass)env->NewGlobalRef(egl_handler_clazz.get()),
                      GetJNIDestructorFunctor(jvm, &JNIEnv::DeleteGlobalRef));
  return true;
}

void SurfaceTextureExtJNI::Unload(JNIEnv *env) {
  egl_handler_get_native_handler_ = nullptr;
  egl_handler_clazz_.reset();
  egl_image_constructor_ = nullptr;
  egl_image_clazz_.reset();
  env->SetStaticLongField(functions_clazz_.get(), functions_handler_, jlong(0));
  functions_handler_ = nullptr;
  env->UnregisterNatives(functions_clazz_.get());
  functions_clazz_.reset();
  hardware_buffer_functions_.reset();
  java_vm_ = nullptr;
}

void *
SurfaceTextureExtJNI::GetEGLHandlerFormEGLObjectHandle(JNIEnv *env,
                                                       jobject egl_object) {
  if (!egl_object) {
    return nullptr;
  }

  return EGLImageKHR(
      env->CallLongMethod(egl_object, egl_handler_get_native_handler_));
}

jobject SurfaceTextureExtJNI::CreateEGLImageFormEGLImageKHR(JNIEnv *env,
                                                            EGLImageKHR image) {
  if (!image) {
    return nullptr;
  }

  return env->NewObject(egl_image_clazz_.get(), egl_image_constructor_,
                        jlong(image));
}

} // namespace hardware_buffer_ext