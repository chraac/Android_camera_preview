package com.example.android.camera.utils

import android.util.Size

interface SurfaceTextureDrawer {
    var viewPortSize: Size

    fun save()
    fun translate(x: Float, y: Float, z: Float)
    fun scale(sx: Float, sy: Float, sz: Float)
    fun rotate(angle: Float, x: Float, y: Float, z: Float)
    fun restore()

    fun draw(surfaceTexture: SurfaceTextureExt)
}