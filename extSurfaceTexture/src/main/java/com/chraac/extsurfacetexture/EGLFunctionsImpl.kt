package com.chraac.extsurfacetexture

import android.annotation.TargetApi
import android.hardware.HardwareBuffer
import android.opengl.EGLDisplay

@TargetApi(28)
object EGLFunctionsImpl : EGLFunctions {

    private var native: Long = 0

    init {
        System.loadLibrary("hardware_buffer_ext")
    }

    override fun eglCreateImageFromHardwareBuffer(
            display: EGLDisplay?, hardwareBuffer: HardwareBuffer?): EGLImageKHR? =
            nativeCreateImageFromHardwareBuffer(native, display, hardwareBuffer)

    override fun eglDestroyImageKHR(display: EGLDisplay?, image: EGLImageKHR?) =
            nativeDestroyImageKHR(native, display, image)

    override fun glEGLImageTargetTexture2DOES(target: Int, image: EGLImageKHR?) =
            nativeEGLImageTargetTexture2DOES(native, target, image)

    private external fun nativeCreateImageFromHardwareBuffer(
            native: Long, display: EGLDisplay?, hardwareBuffer: HardwareBuffer?): EGLImageKHR?

    private external fun nativeDestroyImageKHR(native: Long, display: EGLDisplay?, image: EGLImageKHR?)
    private external fun nativeEGLImageTargetTexture2DOES(native: Long, target: Int, image: EGLImageKHR?)
}