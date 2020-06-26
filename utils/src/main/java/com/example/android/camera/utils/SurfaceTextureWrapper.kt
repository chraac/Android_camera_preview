package com.example.android.camera.utils

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20.*
import android.util.Size
import androidx.annotation.WorkerThread

class SurfaceTextureWrapper : SurfaceTextureExt {

    override val target: Int
        get() = GL_TEXTURE_EXTERNAL_OES

    override val surfaceTexture: SurfaceTexture
        get() = _surfaceTexture

    override var size: Size
        get() = _size
        set(value) {
            if (value != _size) {
                _surfaceTexture.setDefaultBufferSize(value.width, value.height)
            }

            _size = value
        }

    override val textureId: Int
        get() = _textureId[0]

    private val _surfaceTexture = SurfaceTexture(0).apply {
        // detach SurfaceTexture form current thread cause we will draw in render thread later
        detachFromGLContext()
    }

    private var _size = Size(0, 0)
    private var _textureId = IntArray(1)

    @WorkerThread
    override fun bind(drawer: SurfaceTextureDrawer): Int {
        if (_textureId[0] == 0) {
            glEnable(GL_TEXTURE_EXTERNAL_OES)
            glGenTextures(_textureId.size, _textureId, 0)
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(target, _textureId[0])
            glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(target, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            glTexParameteri(target, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(target, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            _surfaceTexture.attachToGLContext(_textureId[0])
        }

        _surfaceTexture.updateTexImage()
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