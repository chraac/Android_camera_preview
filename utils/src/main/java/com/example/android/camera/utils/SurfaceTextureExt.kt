package com.example.android.camera.utils

import android.graphics.SurfaceTexture
import android.util.Size
import androidx.annotation.WorkerThread

@WorkerThread
interface SurfaceTextureExt {

    val target: Int
    val surfaceTexture: SurfaceTexture
    var size: Size
    val textureId: Int

    @WorkerThread
    fun createGLTexture()

    @WorkerThread
    fun bind(drawer: SurfaceTextureDrawer): Int

    @WorkerThread
    fun unbind()

    @WorkerThread
    fun deleteGLTexture()
}