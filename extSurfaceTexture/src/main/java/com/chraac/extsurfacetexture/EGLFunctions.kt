package com.chraac.extsurfacetexture

import android.hardware.HardwareBuffer
import android.opengl.EGLDisplay

interface EGLFunctions {

    fun eglCreateImageFromHardwareBuffer(
            display: EGLDisplay?, hardwareBuffer: HardwareBuffer?): EGLImageKHR?

    fun eglDestroyImageKHR(display: EGLDisplay?, image: EGLImageKHR?)

    fun glEGLImageTargetTexture2DOES(target: Int, image: EGLImageKHR?)

}