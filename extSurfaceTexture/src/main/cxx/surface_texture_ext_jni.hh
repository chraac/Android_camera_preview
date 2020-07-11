
#ifndef __SURFACE_TEXTURE_EXT_JNI_H__
#define __SURFACE_TEXTURE_EXT_JNI_H__

#include "hardware_buffer_ext.hh"
#include <functional>
#include <jni.h>
#include <memory>

namespace hardware_buffer_ext {

class SurfaceTextureExtJNI {
public:
  typedef std::unique_ptr<_jclass, std::function<void(_jclass *)>>
      UniquePtrJClass;

  static SurfaceTextureExtJNI &GetInstance() {
    static SurfaceTextureExtJNI s_instance;
    return s_instance;
  }

  bool IsValid() const { return java_vm_; }
  bool Load(JavaVM *jvm, JNIEnv *env);
  void Unload(JNIEnv *env);

  void *GetEGLHandlerFormEGLObjectHandle(JNIEnv *env, jobject image);
  jobject CreateEGLImageFormEGLImageKHR(JNIEnv *env, EGLImageKHR image);

private:
  SurfaceTextureExtJNI() = default;
  ~SurfaceTextureExtJNI() = default;

  std::unique_ptr<HardwareBufferFunctions> hardware_buffer_functions_;
  JavaVM *java_vm_ = nullptr;
  UniquePtrJClass functions_clazz_;
  jfieldID functions_handler_ = nullptr;
  UniquePtrJClass egl_image_clazz_;
  jmethodID egl_image_constructor_ = nullptr;
  UniquePtrJClass egl_handler_clazz_;
  jmethodID egl_handler_get_native_handler_ = nullptr;

  SurfaceTextureExtJNI(const SurfaceTextureExtJNI &) = delete;

  void operator=(const SurfaceTextureExtJNI &) = delete;
};

} // namespace hardware_buffer_ext

#endif //__SURFACE_TEXTURE_EXT_JNI_H__
