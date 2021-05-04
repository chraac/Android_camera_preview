#ifndef __COM_CHRAAC_ADVSURFACETEXTURE_SURFACEEXT_H__
#define __COM_CHRAAC_ADVSURFACETEXTURE_SURFACEEXT_H__

#include "jni_helper.hh"
#include <jni.h>

namespace adv_surface_texture {

class JNISurfaceExt : public Singleton<JNISurfaceExt> {
  friend class Singleton<JNISurfaceExt>;

public:
  bool IsValid() const { return java_vm_; }
  bool Load(JavaVM *jvm, JNIEnv *env);
  void Unload(JNIEnv *env);

private:
  JNISurfaceExt() = default;
  ~JNISurfaceExt() = default;

  JavaVM *java_vm_ = nullptr;
  UniquePtrJClass ext_clazz_;
};

} // namespace adv_surface_texture

#endif // __COM_CHRAAC_ADVSURFACETEXTURE_SURFACEEXT_H__
