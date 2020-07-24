package com.chraac.camera.preview.utils

import android.graphics.SurfaceTexture
import android.os.Handler
import android.util.Size
import android.view.Surface
import com.chraac.advsurfacetexture.SurfaceTextureProvider


class SurfaceTextureProviderImpl(
        width: Int,
        height: Int
) : SurfaceTextureProvider,
        SurfaceTexture.OnFrameAvailableListener {

    override val timestamp: Long
        get() = _surfaceTexture.timestamp

    override val surface: Surface by lazy {
        Surface(_surfaceTexture)
    }

    override val size: Size = Size(width, height)

    private val _surfaceTexture = SurfaceTexture(0).apply {
        // detach SurfaceTexture form current thread cause we will draw in render thread later
        detachFromGLContext()
        setDefaultBufferSize(width, height)
    }

    private var _listener: SurfaceTextureProvider.OnFrameAvailableListener? = null

    override fun setOnFrameAvailableListener(
            listener: SurfaceTextureProvider.OnFrameAvailableListener?, handler: Handler?) {
        if (listener != null) {
            _listener = listener
            _surfaceTexture.setOnFrameAvailableListener(this, handler)
        } else {
            _surfaceTexture.setOnFrameAvailableListener(null)
            _listener = null
        }
    }

    override fun attachToGLContext(texName: Int) = _surfaceTexture.attachToGLContext(texName)

    override fun updateTexImage() = _surfaceTexture.updateTexImage()

    override fun releaseTexImage() = _surfaceTexture.releaseTexImage()

    override fun detachFromGLContext() = _surfaceTexture.detachFromGLContext()

    override fun close() = _surfaceTexture.release()

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        _listener?.onFrameAvailable(this)
    }

}