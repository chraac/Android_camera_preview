package com.example.android.camera.utils

import android.util.Size
import androidx.annotation.WorkerThread

interface SurfaceTextureDrawer {
    var viewPortSize: Size
        @WorkerThread set

    @WorkerThread
    fun save()

    @WorkerThread
    fun translate(x: Float, y: Float, z: Float)

    @WorkerThread
    fun scale(sx: Float, sy: Float, sz: Float)

    @WorkerThread
    fun rotate(angle: Float, x: Float, y: Float, z: Float)

    @WorkerThread
    fun restore()

    @WorkerThread
    fun draw(surfaceTexture: SurfaceTextureExt)
}