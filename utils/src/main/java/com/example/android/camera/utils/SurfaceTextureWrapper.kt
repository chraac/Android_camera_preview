package com.example.android.camera.utils

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.util.Size

class SurfaceTextureWrapper : SurfaceTextureExt {

    override val target: Int
        get() = GLES11Ext.GL_TEXTURE_EXTERNAL_OES

    override val surfaceTexture: SurfaceTexture
        get() = _surfaceTexture

    override var size: Size
        get() = _size
        set(value) {
            _size = value
        }

    override var textureId: Int
        get() = _textureId
        set(value) {
            _surfaceTexture.attachToGLContext(_textureId)
            _textureId = value
        }

    private val _surfaceTexture = SurfaceTexture(0)
    private var _size = Size(0, 0)
    private var _textureId: Int = 0

    override fun bind(drawer: SurfaceTextureDrawer): Int {
        return _textureId
    }

    override fun unbind() {
    }
}