package com.chraac.advsurfacetexture

import android.os.Handler
import android.view.Surface
import androidx.annotation.*
import androidx.annotation.IntRange

interface SurfaceTextureProvider : AutoCloseable {

    @IntDef(value = [
        Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270])
    annotation class Rotation

    interface OnFrameAvailableListener {
        @WorkerThread
        fun onFrameAvailable(surfaceTexture: SurfaceTextureProvider)
    }

    interface SurfaceRotationProvider {
        @Rotation
        @WorkerThread
        fun getSurfaceRotationFromImage(imageReader: ImageReader, image: ImageReader.Image): Int
    }

    val timestamp: Long

    val surface: Surface

    @get:IntRange(from = 1)
    val width: Int

    @get:IntRange(from = 1)
    val height: Int

    @MainThread
    fun setOnFrameAvailableListener(listener: OnFrameAvailableListener?, handler: Handler?)

    @MainThread
    fun setSurfaceRotationProvider(rotationProvider: SurfaceRotationProvider?)

    @WorkerThread
    fun getTransformMatrix(@Size(value = 16) mtx: FloatArray)

    @WorkerThread
    fun attachToGLContext(texName: Int)

    @WorkerThread
    fun updateTexImage()

    @WorkerThread
    fun releaseTexImage()

    @WorkerThread
    fun detachFromGLContext()

}