package com.example.android.camera.utils

import android.graphics.SurfaceTexture
import android.util.Size

interface SurfaceTextureDrawer {
    var viewPortSize: Size
    fun draw(surfaceTexture: SurfaceTextureExt)
}