package com.chraac.advsurfacetexture

import android.annotation.TargetApi
import android.hardware.HardwareBuffer
import android.opengl.EGLDisplay

@TargetApi(26)
object EGLFunctionsImpl : EGLFunctions, NativeObject {

    override var native: Long = 0

    init {
        System.loadLibrary("adv_surface_texture")
    }

    override fun eglCreateImageKHR(display: EGLDisplay?, image: ImageReader.Image?): EGLImageKHR? =
            nativeCreateImageKHR(native, display, image)

    override fun eglCreateImageFromHardwareBuffer(
            display: EGLDisplay?, hardwareBuffer: HardwareBuffer?): EGLImageKHR? =
            nativeCreateImageFromHardwareBuffer(native, display, hardwareBuffer)

    override fun eglDestroyImageKHR(display: EGLDisplay?, image: EGLImageKHR?) =
            nativeDestroyImageKHR(native, display, image)

    override fun glEGLImageTargetTexture2DOES(target: Int, image: EGLImageKHR?) =
            nativeEGLImageTargetTexture2DOES(native, target, image)

    private external fun nativeCreateImageKHR(
            native: Long, display: EGLDisplay?, image: ImageReader.Image?): EGLImageKHR?

    private external fun nativeCreateImageFromHardwareBuffer(
            native: Long, display: EGLDisplay?, hardwareBuffer: HardwareBuffer?): EGLImageKHR?

    private external fun nativeDestroyImageKHR(native: Long, display: EGLDisplay?, image: EGLImageKHR?)
    private external fun nativeEGLImageTargetTexture2DOES(native: Long, target: Int, image: EGLImageKHR?)
}