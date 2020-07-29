package com.chraac.advsurfacetexture

import android.os.Handler
import android.view.Surface
import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread

interface SurfaceTextureProvider : AutoCloseable {

    interface OnFrameAvailableListener {
        @WorkerThread
        fun onFrameAvailable(surfaceTexture: SurfaceTextureProvider)
    }

    val timestamp: Long

    val surface: Surface

    @get:IntRange(from = 1)
    val width: Int

    @get:IntRange(from = 1)
    val height: Int

    @MainThread
    fun setOnFrameAvailableListener(listener: OnFrameAvailableListener?, handler: Handler?)

    @WorkerThread
    fun getTransformMatrix(mtx: FloatArray)

    @WorkerThread
    fun attachToGLContext(texName: Int)

    @WorkerThread
    fun updateTexImage()

    @WorkerThread
    fun releaseTexImage()

    @WorkerThread
    fun detachFromGLContext()

}