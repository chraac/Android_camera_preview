
#ifndef __COM_CHRAAC_ADVSURFACETEXTURE_EGLFUNCTIONSIMPL_H__
#define __COM_CHRAAC_ADVSURFACETEXTURE_EGLFUNCTIONSIMPL_H__

#include "hardware_buffer_ext.hh"
#include "jni_helper.hh"
#include <jni.h>

namespace adv_surface_texture {

class JNIAdvSurfaceTexture : public Singleton<JNIAdvSurfaceTexture> {
  friend class Singleton<JNIAdvSurfaceTexture>;

public:
  bool IsValid() const { return java_vm_; }
  bool Load(JavaVM *jvm, JNIEnv *env);
  void Unload(JNIEnv *env);

  void *GetEGLHandlerFormEGLObjectHandle(JNIEnv *env, jobject image);
  jobject CreateEGLImageFormEGLImageKHR(JNIEnv *env, EGLImageKHR image);

private:
  JNIAdvSurfaceTexture() = default;
  ~JNIAdvSurfaceTexture() = default;

  std::unique_ptr<HardwareBufferFunctions> hardware_buffer_functions_;
  JavaVM *java_vm_ = nullptr;
  UniquePtrJClass functions_clazz_;
  jfieldID functions_handler_ = nullptr;
  UniquePtrJClass egl_image_clazz_;
  jmethodID egl_image_constructor_ = nullptr;
  UniquePtrJClass egl_handler_clazz_;
  jmethodID egl_handler_get_native_handler_ = nullptr;

  JNIAdvSurfaceTexture(const JNIAdvSurfaceTexture &) = delete;
  void operator=(const JNIAdvSurfaceTexture &) = delete;
};

} // namespace adv_surface_texture

#endif //__SURFACE_TEXTURE_EXT_JNI_H__
