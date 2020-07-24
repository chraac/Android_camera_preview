
#include "com_chraac_advsurfacetexture_NativeImageReader.hh"
#include "com_chraac_advsurfacetexture_NativeObject.hh"
#include <android/hardware_buffer_jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>
#include <media/NdkImageReader.h>

namespace {

using namespace adv_surface_texture;

constexpr const char *kLogTag = "JNINativeImage";
constexpr const char *kNativeImageReaderClassName =
    "com/chraac/advsurfacetexture/NativeImageReader";
constexpr const char *kNativeImageClassName =
    "com/chraac/advsurfacetexture/NativeImageReader$NativeImage";

/*
 * Class:     com_chraac_advsurfacetexture_NativeImageReader_NativeImage
 * Method:    nativeGetHardwareBuffer
 * Signature: (J)Landroid/hardware/HardwareBuffer;
 */
jobject JNINativeImage_getHardwareBuffer(JNIEnv *env, jobject, jlong native) {
  auto *image = reinterpret_cast<AImage *>(native);
  if (!image) {
    return nullptr;
  }

  AHardwareBuffer *hardware_buffer = nullptr;
  auto status = AImage_getHardwareBuffer(image, &hardware_buffer);
  if (status != AMEDIA_OK || !hardware_buffer) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag,
                        "Get AHardwareBuffer failed: %d.", int(status));
    return nullptr;
  }

  return AHardwareBuffer_toHardwareBuffer(env, hardware_buffer);
}

/*
 * Class:     com_chraac_advsurfacetexture_NativeImageReader_NativeImage
 * Method:    nativeClose
 * Signature: (J)V
 */
void JNINativeImage_close(JNIEnv *env, jobject object, jlong native) {
  auto *image = reinterpret_cast<AImage *>(native);
  if (image) {
    JNINativeObject::GetInstance().SetNativeField(env, object, nullptr);
    AImage_delete(image);
  }
}

const JNINativeMethod g_native_image_methods[] = {
    {"nativeGetHardwareBuffer", "(J)Landroid/hardware/HardwareBuffer;",
     (void *)JNINativeImage_getHardwareBuffer},
    {"nativeClose", "(J)V", (void *)JNINativeImage_close},
};

class JNINativeImageReaderContext {
public:
  explicit JNINativeImageReaderContext(JavaVM *jvm, JNIEnv *env, jobject object,
                                       AImageReader *image_reader) {
    image_reader_object_ = UniquePtrJObject(
        env->NewGlobalRef(object),
        GetJNIDestructorFunctor(jvm, &JNIEnv::DeleteGlobalRef));
    image_reader_ = image_reader;
  }

  ~JNINativeImageReaderContext() {
    if (image_reader_) {
      AImageReader_delete(image_reader_);
    }
  }

  AImageReader *image_reader() const { return image_reader_; }
  jobject image_reader_object() const { return image_reader_object_.get(); }

  JNINativeImageReaderContext(const JNINativeImageReaderContext &) = delete;
  void operator=(const JNINativeImageReaderContext &) = delete;

private:
  UniquePtrJObject image_reader_object_;
  AImageReader *image_reader_ = nullptr;
};

/*
 * Class:     com_chraac_advsurfacetexture_NativeImageReader
 * Method:    nativeInit
 * Signature: (IIII)V
 */
void JNINativeImageReader_init(JNIEnv *env, jobject object, jint width,
                               jint height, jint format, jint max_image) {
  JavaVM *jvm = nullptr;
  if (env->GetJavaVM(&jvm) != 0 || !jvm) {
    return;
  }

  AImageReader *image_reader = nullptr;
  if (AImageReader_new(int32_t(width), int32_t(height), int32_t(format),
                       int32_t(max_image), &image_reader) != AMEDIA_OK ||
      !image_reader) {
    return;
  }

  auto *context =
      new JNINativeImageReaderContext(jvm, env, object, image_reader);
  JNINativeObject::GetInstance().SetNativeField(env, object, context);
}

/*
 * Class:     com_chraac_advsurfacetexture_NativeImageReader
 * Method:    nativeGetSurface
 * Signature: (J)Landroid/view/Surface;
 */
jobject JNINativeImageReader_getSurface(JNIEnv *env, jobject, jlong native) {
  auto *context = reinterpret_cast<JNINativeImageReaderContext *>(native);
  if (!context) {
    return nullptr;
  }

  ANativeWindow *native_window = nullptr;
  auto status = AImageReader_getWindow(context->image_reader(), &native_window);
  if (status != AMEDIA_OK || !native_window) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag,
                        "Get ANativeWindow failed: %d.", int(status));
    return nullptr;
  }

  return ANativeWindow_toSurface(env, native_window);
}

/*
 * Class:     com_chraac_advsurfacetexture_NativeImageReader
 * Method:    nativeAcquireLatestImage
 * Signature: (J)Lcom/chraac/advsurfacetexture/NativeImageReader$NativeImage;
 */
jobject JNINativeImageReader_acquireLatestImage(JNIEnv *env, jobject,
                                                jlong native) {
  auto *context = reinterpret_cast<JNINativeImageReaderContext *>(native);
  if (!context) {
    return nullptr;
  }

  AImage *image = nullptr;
  auto status =
      AImageReader_acquireLatestImage(context->image_reader(), &image);
  if (status != AMEDIA_OK || !image) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag,
                        "Acquire latest AImage failed: %d.", int(status));
    return nullptr;
  }

  return JNINativeImage::GetInstance().AImage_toNativeImage(env, image);
}

/*
 * Class:     com_chraac_advsurfacetexture_NativeImageReader
 * Method:    nativeAcquireNextImage
 * Signature: (J)Lcom/chraac/advsurfacetexture/NativeImageReader$NativeImage;
 */
jobject JNINativeImageReader_acquireNextImage(JNIEnv *env, jobject,
                                              jlong native) {
  auto *context = reinterpret_cast<JNINativeImageReaderContext *>(native);
  if (!context) {
    return nullptr;
  }

  AImage *image = nullptr;
  auto status = AImageReader_acquireNextImage(context->image_reader(), &image);
  if (status != AMEDIA_OK || !image) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag,
                        "Acquire latest AImage failed: %d.", int(status));
    return nullptr;
  }

  return JNINativeImage::GetInstance().AImage_toNativeImage(env, image);
}

/*
 * Class:     com_chraac_advsurfacetexture_NativeImageReader
 * Method:    nativeEnableImageAvailableListener
 * Signature: (JZ)V
 */
void JNINativeImageReader_enableImageAvailableListener(JNIEnv *, jobject,
                                                       jlong native,
                                                       jboolean enabled) {
  auto *context = reinterpret_cast<JNINativeImageReaderContext *>(native);
  if (!context) {
    return;
  }

  if (enabled == JNI_TRUE) {
    AImageReader_ImageListener listener = {
        context, [](void *ctx, AImageReader *) {
          auto *context = reinterpret_cast<JNINativeImageReaderContext *>(ctx);
          JNINativeImage::GetInstance().NotifyImageAvailable(
              context->image_reader_object());
        }};

    AImageReader_setImageListener(context->image_reader(), &listener);
  } else {
    AImageReader_setImageListener(context->image_reader(), nullptr);
  }
}

/*
 * Class:     com_chraac_advsurfacetexture_NativeImageReader
 * Method:    nativeClose
 * Signature: (J)V
 */
void JNINativeImageReader_close(JNIEnv *env, jobject object, jlong native) {
  auto *context = reinterpret_cast<JNINativeImageReaderContext *>(native);
  if (context) {
    JNINativeObject::GetInstance().SetNativeField(env, object, nullptr);
    delete context;
  }
}

const JNINativeMethod g_native_image_reader_methods[] = {
    {"nativeInit", "(IIII)V", (void *)JNINativeImageReader_init},
    {"nativeGetSurface", "(J)Landroid/view/Surface;",
     (void *)JNINativeImageReader_getSurface},
    {"nativeAcquireLatestImage",
     "(J)Lcom/chraac/advsurfacetexture/NativeImageReader$NativeImage;",
     (void *)JNINativeImageReader_acquireLatestImage},
    {"nativeAcquireNextImage",
     "(J)Lcom/chraac/advsurfacetexture/NativeImageReader$NativeImage;",
     (void *)JNINativeImageReader_acquireNextImage},
    {"nativeEnableImageAvailableListener", "(JZ)V",
     (void *)JNINativeImageReader_enableImageAvailableListener},
    {"nativeClose", "(J)V", (void *)JNINativeImageReader_close},
}; // namespace

} // namespace

namespace adv_surface_texture {

bool JNINativeImage::Load(JavaVM *jvm, JNIEnv *env) {
  UniquePtrJClass image_reader_clazz(
      env->FindClass(kNativeImageReaderClassName),
      GetJNIDestructorFunctor(jvm, &JNIEnv::DeleteLocalRef));
  UniquePtrJClass image_clazz(
      env->FindClass(kNativeImageClassName),
      GetJNIDestructorFunctor(jvm, &JNIEnv::DeleteLocalRef));
  if (!image_reader_clazz || !image_clazz) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag, "FindClass failed.");
    return false;
  }

  image_reader_image_available_ =
      env->GetMethodID(image_reader_clazz.get(), "notifyImageAvailable", "()V");

  image_constructor_ =
      env->GetMethodID(image_clazz.get(), "<init>", "(IIIJJ)V");

  if (env->RegisterNatives(image_reader_clazz.get(),
                           g_native_image_reader_methods,
                           sizeof(g_native_image_reader_methods) /
                               sizeof(g_native_image_reader_methods[0]))) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag,
                        "RegisterNatives for NativeImageReader failed.");
    return false;
  }

  if (env->RegisterNatives(image_clazz.get(), g_native_image_methods,
                           sizeof(g_native_image_methods) /
                               sizeof(g_native_image_methods[0]))) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag,
                        "RegisterNatives for NativeImage failed.");
    return false;
  }

  image_reader_clazz_ =
      UniquePtrJClass((jclass)env->NewGlobalRef(image_reader_clazz.get()),
                      GetJNIDestructorFunctor(jvm, &JNIEnv::DeleteGlobalRef));
  image_clazz_ =
      UniquePtrJClass((jclass)env->NewGlobalRef(image_clazz.get()),
                      GetJNIDestructorFunctor(jvm, &JNIEnv::DeleteGlobalRef));
  java_vm_ = jvm;
  return true;
}

void JNINativeImage::Unload(JNIEnv *env) {
  env->UnregisterNatives(image_clazz_.get());
  image_constructor_ = nullptr;
  image_clazz_.reset();
  env->UnregisterNatives(image_reader_clazz_.get());
  image_reader_image_available_ = nullptr;
  image_reader_clazz_.reset();
  java_vm_ = nullptr;
}

jobject JNINativeImage::AImage_toNativeImage(JNIEnv *env, AImage *image) {
  if (!IsValid()) {
    return nullptr;
  }

  int32_t width = 0;
  auto status = AImage_getWidth(image, &width);
  if (status != AMEDIA_OK) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag,
                        "Get width of AImage failed: %d", int(status));
    return nullptr;
  }

  int32_t height = 0;
  status = AImage_getHeight(image, &height);
  if (status != AMEDIA_OK) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag,
                        "Get height of AImage failed: %d", int(status));
    return nullptr;
  }

  /*
   * 0 for UNKNOWN format
   * Reference:
   * https://developer.android.com/reference/android/graphics/ImageFormat#UNKNOWN
   */
  int32_t format = 0;
  status = AImage_getFormat(image, &format);
  if (status != AMEDIA_OK) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag,
                        "Get format of AImage failed: %d", int(status));
    return nullptr;
  }

  int64_t timestamp = 0;
  AImage_getTimestamp(image, &timestamp);
  return env->NewObject(image_clazz_.get(), image_constructor_, jint(width),
                        jint(height), jint(format), jlong(timestamp),
                        jlong(image));
}

void JNINativeImage::NotifyImageAvailable(jobject object) {
  if (!IsValid()) {
    return;
  }

  JNIEnvHelper env(java_vm_);
  env->CallVoidMethod(object, image_reader_image_available_);
}

} // namespace adv_surface_texture