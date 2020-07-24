
#ifndef CAMERA_PREVIEW_GIT_JNI_HELPER_HH
#define CAMERA_PREVIEW_GIT_JNI_HELPER_HH

#include <functional>
#include <jni.h>
#include <memory>

namespace adv_surface_texture {

template <typename TyParam>
inline std::function<void(TyParam)>
GetJNIDestructorFunctor(JavaVM *jvm, void (JNIEnv::*deleter)(TyParam)) {
  return [jvm, deleter](TyParam object) {
    JNIEnv *env = nullptr;
    if (jvm->GetEnv((void **)&env, JNI_VERSION_1_6) == JNI_OK) {
      (*env.*deleter)(object);
    }
  };
}

typedef std::unique_ptr<_jclass, decltype(GetJNIDestructorFunctor<jclass>(
                                     nullptr, nullptr))>
    UniquePtrJClass;

typedef std::unique_ptr<_jobject, decltype(GetJNIDestructorFunctor<jobject>(
                                      nullptr, nullptr))>
    UniquePtrJObject;

class JNIEnvHelper {
public:
  explicit JNIEnvHelper(JavaVM *jvm) : jvm_(jvm) {}

  ~JNIEnvHelper() {
    if (need_detach_ && jni_env_) {
      jvm_->DetachCurrentThread();
    }
  }

  JNIEnv *Get() {
    if (!jni_env_) {
      if (jvm_->GetEnv((void **)&jni_env_, JNI_VERSION_1_6) != JNI_OK) {
        JavaVMAttachArgs args = {JNI_VERSION_1_6, NULL, NULL};
        jvm_->AttachCurrentThread(&jni_env_, &args);
        need_detach_ = true;
      }
    }

    return jni_env_;
  }

  JNIEnv *operator->() { return Get(); }
  operator JNIEnv *() { return Get(); }

private:
  JNIEnvHelper(const JNIEnvHelper &) = delete;
  JNIEnvHelper(JNIEnvHelper &&) = delete;
  void operator=(const JNIEnvHelper &) = delete;
  void operator=(JNIEnvHelper &&) = delete;

  JavaVM *jvm_ = nullptr;
  JNIEnv *jni_env_ = nullptr;
  bool need_detach_ = false;
};

template <typename TySuper> class Singleton {
public:
  static TySuper &GetInstance() {
    static TySuper s_instance;
    return s_instance;
  }

protected:
  Singleton() = default;

  ~Singleton() = default;

  Singleton(const Singleton &) = delete;

  void operator=(const Singleton &) = delete;
};

} // namespace adv_surface_texture

#endif // CAMERA_PREVIEW_GIT_JNI_HELPER_HH
