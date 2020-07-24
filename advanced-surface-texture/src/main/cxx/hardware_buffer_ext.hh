
#ifndef __HARDWARE_BUFFER_EXT_H__
#define __HARDWARE_BUFFER_EXT_H__

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>
#include <android/hardware_buffer.h>

namespace adv_surface_texture {

typedef EGLImageKHR GLeglImageOES;

typedef EGLClientBuffer(EGLAPIENTRYP PFNEGLGETNATIVECLIENTBUFFERANDROIDPROC)(
    const AHardwareBuffer *buffer);

typedef void(GL_APIENTRYP PFNGLEGLIMAGETARGETTEXTURE2DOESPROC)(
    GLenum target, GLeglImageOES image);

class HardwareBufferFunctions {
public:
  HardwareBufferFunctions();
  ~HardwareBufferFunctions();

  bool IsValid() const {
    return get_native_client_buffer_ && create_image_khr_ &&
           destroy_image_khr_ && image_target_texture2d_oes_;
  }

  EGLClientBuffer
  SoftLinkGetNativeClientBufferANDROID(const AHardwareBuffer *buffer);

  EGLImageKHR SoftLinkCreateImageKHR(EGLDisplay dpy, EGLContext ctx,
                                     EGLenum target, EGLClientBuffer buffer,
                                     const EGLint *attrib_list);

  EGLBoolean SoftLinkDestroyImageKHR(EGLDisplay dpy, EGLImageKHR image);

  void SoftLinkEGLImageTargetTexture2DOES(GLenum target, GLeglImageOES image);

private:
  PFNEGLGETNATIVECLIENTBUFFERANDROIDPROC get_native_client_buffer_ = nullptr;
  PFNEGLCREATEIMAGEKHRPROC create_image_khr_ = nullptr;
  PFNEGLDESTROYIMAGEKHRPROC destroy_image_khr_ = nullptr;
  PFNGLEGLIMAGETARGETTEXTURE2DOESPROC image_target_texture2d_oes_ = nullptr;

  HardwareBufferFunctions(const HardwareBufferFunctions &) = delete;
  HardwareBufferFunctions(HardwareBufferFunctions &&) = delete;
  void operator=(const HardwareBufferFunctions &) = delete;
  void operator=(HardwareBufferFunctions &&) = delete;
};

} // namespace adv_surface_texture

#endif // __HARDWARE_BUFFER_EXT_H__
