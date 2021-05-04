package com.chraac.advsurfacetexture

import android.view.Surface

object SurfaceExt {

    init {
        System.loadLibrary("adv_surface_texture")
    }

    fun setBuffersGeometry(
        surface: Surface,
        width: Int,
        height: Int,
        format: Int
    ) = nativeSetBuffersGeometry(surface, width, height, format)

    private external fun nativeSetBuffersGeometry(
        surface: Surface,
        width: Int,
        height: Int,
        format: Int
    ): Boolean

}

fun Surface.setBuffersGeometry(width: Int, height: Int, format: Int) =
    SurfaceExt.setBuffersGeometry(this, width, height, format)