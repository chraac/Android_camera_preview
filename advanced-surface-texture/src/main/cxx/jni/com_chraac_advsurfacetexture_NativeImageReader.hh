
#ifndef __COM_CHRAAC_ADVSURFACETEXTURE_NATIVEIMAGEREADER_H__
#define __COM_CHRAAC_ADVSURFACETEXTURE_NATIVEIMAGEREADER_H__

#include "jni_helper.hh"
#include <media/NdkImage.h>

namespace adv_surface_texture {

class JNINativeImage : public Singleton<JNINativeImage> {
  friend class Singleton<JNINativeImage>;

public:
  bool IsValid() const { return java_vm_; }
  bool Load(JavaVM *jvm, JNIEnv *env);
  void Unload(JNIEnv *env);
  jobject AImage_toNativeImage(JNIEnv *env, AImage *image);
  void NotifyImageAvailable(jobject object);

private:
  JNINativeImage() = default;
  ~JNINativeImage() = default;

  JavaVM *java_vm_ = nullptr;
  UniquePtrJClass image_reader_clazz_;
  jmethodID image_reader_image_available_ = nullptr;
  UniquePtrJClass image_clazz_;
  jmethodID image_constructor_ = nullptr;

  JNINativeImage(const JNINativeImage &) = delete;
  void operator=(const JNINativeImage &) = delete;
};

} // namespace adv_surface_texture

#endif //__COM_CHRAAC_ADVSURFACETEXTURE_NATIVEIMAGEREADER_H__
