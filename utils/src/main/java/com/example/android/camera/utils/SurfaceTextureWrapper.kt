package com.example.android.camera.utils

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20.*
import android.util.Size
import androidx.annotation.WorkerThread

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

    override val textureId: Int
        get() = _textureId[0]

    private val _surfaceTexture = SurfaceTexture(0)
    private var _size = Size(0, 0)
    private var _textureId = IntArray(1)

    @WorkerThread
    override fun createGLTexture() {
        glGenTextures(_textureId.size, _textureId, 0)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, _textureId[0])
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        _surfaceTexture.detachFromGLContext()
        _surfaceTexture.attachToGLContext(_textureId[0])
    }

    @WorkerThread
    override fun bind(drawer: SurfaceTextureDrawer): Int {
        return _textureId[0]
    }

    @WorkerThread
    override fun unbind() {
    }

    @WorkerThread
    override fun deleteGLTexture() {
        _surfaceTexture.detachFromGLContext()
        glDeleteTextures(_textureId.size, _textureId, 0)
    }
}