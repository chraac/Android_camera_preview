package com.chraac.advsurfacetexture

import android.os.Handler
import android.util.Size
import android.view.Surface
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread

interface SurfaceTextureProvider : AutoCloseable {

    interface OnFrameAvailableListener {
        @WorkerThread
        fun onFrameAvailable(surfaceTexture: SurfaceTextureProvider)
    }

    val timestamp: Long

    val surface: Surface

    val width: Int

    val height: Int

    @MainThread
    fun setOnFrameAvailableListener(listener: OnFrameAvailableListener?, handler: Handler?)

    @WorkerThread
    fun attachToGLContext(texName: Int)

    @WorkerThread
    fun updateTexImage()

    @WorkerThread
    fun releaseTexImage()

    @WorkerThread
    fun detachFromGLContext()

}