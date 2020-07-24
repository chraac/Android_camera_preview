
#ifndef __COM_CHRAAC_ADVSURFACETEXTURE_NATIVEOBJECT_H__
#define __COM_CHRAAC_ADVSURFACETEXTURE_NATIVEOBJECT_H__

#include "jni_helper.hh"

namespace adv_surface_texture {

class JNINativeObject : public Singleton<JNINativeObject> {
  friend class Singleton<JNINativeObject>;

public:
  bool IsValid() const { return bool(native_object_clazz_); }
  bool Load(JavaVM *jvm, JNIEnv *env);
  void Unload(JNIEnv *env);

  template <typename TyReturn>
  TyReturn *GetNativeField(JNIEnv *env, jobject object) const {
    return reinterpret_cast<TyReturn *>(GetNativeFieldInternal(env, object));
  }

  void SetNativeField(JNIEnv *env, jobject object, void *ptr);

private:
  JNINativeObject() = default;
  ~JNINativeObject() = default;
  void *GetNativeFieldInternal(JNIEnv *env, jobject object) const;

  UniquePtrJClass native_object_clazz_;
  jmethodID native_pointer_getter_ = nullptr;
  jmethodID native_pointer_setter_ = nullptr;

  JNINativeObject(const JNINativeObject &) = delete;
  void operator=(const JNINativeObject &) = delete;
};

} // namespace adv_surface_texture

#endif // __COM_CHRAAC_ADVSURFACETEXTURE_NATIVEOBJECT_H__
