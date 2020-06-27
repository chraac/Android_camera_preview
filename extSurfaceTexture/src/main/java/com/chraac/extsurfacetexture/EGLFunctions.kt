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
            nativeCreateImageFromHardwareBuffer(display, hardwareBuffer)

    fun eglDestroyImageKHR(display: EGLDisplay, image: EGLImage?) =
            nativeDestroyImageKHR(display, image)

    fun eglImageTargetTexture2DOES(target: Int, image: EGLImage?) =
            nativeImageTargetTexture2DOES(target, image)

    private external fun nativeCreateImageFromHardwareBuffer(
            display: EGLDisplay, hardwareBuffer: HardwareBuffer?): EGLImage?

    private external fun nativeDestroyImageKHR(display: EGLDisplay, image: EGLImage?)
    private external fun nativeImageTargetTexture2DOES(target: Int, image: EGLImage?)
}