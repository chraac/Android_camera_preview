
#include "hardware_buffer_ext.hh"

namespace adv_surface_texture {

HardwareBufferFunctions::HardwareBufferFunctions() {
  get_native_client_buffer_ =
      (PFNEGLGETNATIVECLIENTBUFFERANDROIDPROC)eglGetProcAddress(
          "eglGetNativeClientBufferANDROID");
  create_image_khr_ =
      (PFNEGLCREATEIMAGEKHRPROC)eglGetProcAddress("eglCreateImageKHR");
  destroy_image_khr_ =
      (PFNEGLDESTROYIMAGEKHRPROC)eglGetProcAddress("eglDestroyImageKHR");
  image_target_texture2d_oes_ =
      (PFNGLEGLIMAGETARGETTEXTURE2DOESPROC)eglGetProcAddress(
          "glEGLImageTargetTexture2DOES");
}

HardwareBufferFunctions::~HardwareBufferFunctions() {
  get_native_client_buffer_ = nullptr;
  create_image_khr_ = nullptr;
  destroy_image_khr_ = nullptr;
  image_target_texture2d_oes_ = nullptr;
}

EGLClientBuffer HardwareBufferFunctions::SoftLinkGetNativeClientBufferANDROID(
    const AHardwareBuffer *buffer) {
  return get_native_client_buffer_ ? get_native_client_buffer_(buffer)
                                   : nullptr;
}

EGLImageKHR HardwareBufferFunctions::SoftLinkCreateImageKHR(
    EGLDisplay dpy, EGLContext ctx, EGLenum target, EGLClientBuffer buffer,
    const EGLint *attrib_list) {
  return create_image_khr_
             ? create_image_khr_(dpy, ctx, target, buffer, attrib_list)
             : nullptr;
}

EGLBoolean HardwareBufferFunctions::SoftLinkDestroyImageKHR(EGLDisplay dpy,
                                                            EGLImageKHR image) {
  return destroy_image_khr_ ? destroy_image_khr_(dpy, image) : EGL_FALSE;
}

void HardwareBufferFunctions::SoftLinkEGLImageTargetTexture2DOES(
    GLenum target, GLeglImageOES image) {
  if (image_target_texture2d_oes_) {
    image_target_texture2d_oes_(target, image);
  }
}

} // namespace adv_surface_texture