package com.chraac.camera.preview.utils

import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20.*
import android.os.Handler
import android.util.Size
import android.view.Surface
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.chraac.advsurfacetexture.SurfaceTextureProvider

class SurfaceTextureManagerImpl(
        surfaceTexture: SurfaceTextureProvider
) : SurfaceTextureManager, SurfaceTextureProvider.OnFrameAvailableListener {

    override val target: Int
        get() = GL_TEXTURE_EXTERNAL_OES

    override val surface: Surface
        get() = _surfaceTexture.surface

    override val textureId: Int
        get() = _textureId[0]

    override val size: Size
        get() = _surfaceTexture.size

    private val _surfaceTexture = surfaceTexture

    private var _textureId = IntArray(1)
    private var _listener: SurfaceTextureManager.FrameAvailableListener? = null

    @MainThread
    override fun setFrameAvailableListener(listener: SurfaceTextureManager.FrameAvailableListener?,
                                           handler: Handler?) {
        if (listener != null) {
            _listener = listener
            _surfaceTexture.setOnFrameAvailableListener(this, handler)
        } else {
            _surfaceTexture.setOnFrameAvailableListener(null, null)
            _listener = null
        }
    }

    @WorkerThread
    override fun bind(drawer: SurfaceTextureDrawer): Int {
        if (_textureId[0] == 0) {
            checkGLError()
            glGenTextures(_textureId.size, _textureId, 0)
            checkGLError()
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(target, _textureId[0])
            checkGLError()
            glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(target, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            glTexParameteri(target, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(target, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            checkGLError()
            _surfaceTexture.attachToGLContext(_textureId[0])
        }

        _surfaceTexture.updateTexImage()
        return _textureId[0]
    }

    @WorkerThread
    override fun unbind() {
    }

    @WorkerThread
    override fun close() {
        _surfaceTexture.detachFromGLContext()
        glDeleteTextures(_textureId.size, _textureId, 0)
    }

    @WorkerThread
    override fun onFrameAvailable(surfaceTexture: SurfaceTextureProvider) {
        _listener?.onFrameAvailable(this)
    }
}