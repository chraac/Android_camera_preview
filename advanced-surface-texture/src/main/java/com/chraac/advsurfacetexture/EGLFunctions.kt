package com.chraac.advsurfacetexture

import android.hardware.HardwareBuffer
import android.opengl.EGLDisplay

interface EGLFunctions {

    fun eglCreateImageKHR(display: EGLDisplay?, image: ImageReader.Image?): EGLImageKHR?

    fun eglCreateImageFromHardwareBuffer(
            display: EGLDisplay?, hardwareBuffer: HardwareBuffer?): EGLImageKHR?

    fun eglDestroyImageKHR(display: EGLDisplay?, image: EGLImageKHR?)

    fun glEGLImageTargetTexture2DOES(target: Int, image: EGLImageKHR?)

}