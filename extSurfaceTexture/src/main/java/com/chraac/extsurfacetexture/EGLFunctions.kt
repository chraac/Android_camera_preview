package com.chraac.extsurfacetexture

import android.annotation.TargetApi
import android.hardware.HardwareBuffer
import android.opengl.EGLDisplay

@TargetApi(28)
object EGLFunctions {

    private var native: Long = 0

    init {
        System.loadLibrary("hardware_buffer_ext")
    }

    fun eglCreateImageFromHardwareBuffer(display: EGLDisplay, hardwareBuffer: HardwareBuffer?): EGLImage? =
            nativeCreateImageFromHardwareBuffer(native, display, hardwareBuffer)

    fun eglDestroyImageKHR(display: EGLDisplay, image: EGLImage?) =
            nativeDestroyImageKHR(native, display, image)

    fun eglImageTargetTexture2DOES(target: Int, image: EGLImage?) =
            nativeImageTargetTexture2DOES(native, target, image)

    private external fun nativeCreateImageFromHardwareBuffer(
            native: Long, display: EGLDisplay, hardwareBuffer: HardwareBuffer?): EGLImage?

    private external fun nativeDestroyImageKHR(native: Long, display: EGLDisplay, image: EGLImage?)
    private external fun nativeImageTargetTexture2DOES(native: Long, target: Int, image: EGLImage?)
}