package com.chraac.camera.preview.utils

import android.os.Handler
import android.util.Size
import android.view.Surface
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread

@WorkerThread
interface SurfaceTextureManager : AutoCloseable {

    interface FrameAvailableListener {
        fun onFrameAvailable(surfaceTexture: SurfaceTextureManager)
    }

    val target: Int
    val surface: Surface
    val textureId: Int
    val size: Size

    @MainThread
    fun setFrameAvailableListener(listener: FrameAvailableListener?, handler: Handler?)

    @WorkerThread
    fun bind(drawer: SurfaceTextureDrawer): Int

    @WorkerThread
    fun unbind()
}