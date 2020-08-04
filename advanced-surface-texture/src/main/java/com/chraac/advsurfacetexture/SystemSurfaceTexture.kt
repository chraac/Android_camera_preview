package com.chraac.advsurfacetexture

import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.annotation.Size

private val IS_CALLBACK_WITH_HANDLER_AVAILABLE =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

class SystemSurfaceTexture(
        override val width: Int,
        override val height: Int
) : SurfaceTextureProvider,
        SurfaceTexture.OnFrameAvailableListener {

    override val timestamp: Long
        get() = _surfaceTexture.timestamp

    override val surface: Surface by lazy {
        Surface(_surfaceTexture)
    }

    private val _surfaceTexture = SurfaceTexture(0).apply {
        check(width > 0) { "Invalid width" }
        check(height > 0) { "Invalid height" }
        // detach SurfaceTexture form current thread cause we will draw in render thread later
        detachFromGLContext()
        setDefaultBufferSize(width, height)
    }

    @GuardedBy("this")
    private var _listener: SurfaceTextureProvider.OnFrameAvailableListener? = null

    @GuardedBy("this")
    private var _handler: Handler? = null

    private val _onFrameAvailableBlock: () -> Unit = {
        val listener = synchronized(this) {
            _listener
        }

        listener?.onFrameAvailable(this)
    }

    override fun setOnFrameAvailableListener(
            listener: SurfaceTextureProvider.OnFrameAvailableListener?, handler: Handler?) {
        if (listener != null) {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            if (IS_CALLBACK_WITH_HANDLER_AVAILABLE) {
                synchronized(this) {
                    _listener = listener
                }

                _surfaceTexture.setOnFrameAvailableListener(this, handler)
            } else {
                synchronized(this) {
                    _listener = listener
                    _handler = handler
                }

                _surfaceTexture.setOnFrameAvailableListener(this)
            }
        } else {
            _surfaceTexture.setOnFrameAvailableListener(null)
            synchronized(this) {
                _listener = null
                _handler = null
            }
        }
    }

    override fun getTransformMatrix(@Size(value = 16) mtx: FloatArray) =
            _surfaceTexture.getTransformMatrix(mtx)

    override fun attachToGLContext(texName: Int) = _surfaceTexture.attachToGLContext(texName)

    override fun updateTexImage() = _surfaceTexture.updateTexImage()

    override fun releaseTexImage() = _surfaceTexture.releaseTexImage()

    override fun detachFromGLContext() = _surfaceTexture.detachFromGLContext()

    override fun close() {
        synchronized(this) {
            _listener = null
            _handler = null
        }

        _surfaceTexture.release()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        val listener: SurfaceTextureProvider.OnFrameAvailableListener
        val handler: Handler?
        synchronized(this) {
            listener = _listener ?: return
            handler = _handler
        }

        if (handler != null && handler.looper != Looper.myLooper()) {
            handler.post(_onFrameAvailableBlock)
        } else {
            listener.onFrameAvailable(this)
        }
    }

}