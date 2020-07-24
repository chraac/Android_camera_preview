package com.chraac.advsurfacetexture

import android.annotation.TargetApi
import android.hardware.HardwareBuffer
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.annotation.GuardedBy

@TargetApi(26)
class NativeImageReader(
        override val width: Int,
        override val height: Int,
        override val format: Int,
        override val maxImages: Int
) : ImageReader, NativeObject {

    @Suppress("unused")
    private class NativeImage(
            override val width: Int,
            override val height: Int,
            override val format: Int,
            override val timestamp: Long,
            override var native: Long
    ) : ImageReader.Image, NativeObject {

        override val hardwareBuffer: HardwareBuffer?
            get() = nativeGetHardwareBuffer(native)

        override fun close() = nativeClose(native)

        private external fun nativeGetHardwareBuffer(native: Long): HardwareBuffer?
        private external fun nativeClose(native: Long)
    }

    override val surface: Surface by lazy {
        nativeGetSurface(native)
    }

    override var native: Long = 0

    @GuardedBy("this")
    private var _listener: ImageReader.OnImageAvailableListener? = null

    @GuardedBy("this")
    private var _handler: Handler? = null

    private val _listenerBlock: () -> Unit = {
        val listener = synchronized(this@NativeImageReader) {
            _listener
        }

        listener?.onImageAvailable(this@NativeImageReader)
    }

    init {
        System.loadLibrary("adv_surface_texture")

        nativeInit(width, height, format, maxImages)
        check(native != 0L)
    }

    override fun setOnImageAvailableListener(listener: ImageReader.OnImageAvailableListener?,
                                             handler: Handler?) {
        val enabled = synchronized(this) {
            if (listener != null) {
                _listener = listener
                _handler = handler ?: Handler()
                true
            } else {
                _listener = null
                _handler = null
                false
            }
        }

        nativeEnableImageAvailableListener(native, enabled)
    }

    override fun acquireLatestImage(): ImageReader.Image? = nativeAcquireLatestImage(native)

    override fun acquireNextImage(): ImageReader.Image? = nativeAcquireNextImage(native)

    override fun close() {
        nativeClose(native)
        check(native == 0L)
    }

    @Suppress("unused")
    private fun notifyImageAvailable() {
        val handler: Handler
        val listener: ImageReader.OnImageAvailableListener
        synchronized(this) {
            listener = _listener ?: return
            handler = _handler ?: return
        }

        if (handler.looper == Looper.myLooper()) {
            listener.onImageAvailable(this)
        } else {
            handler.post(_listenerBlock)
        }
    }

    private external fun nativeInit(width: Int, height: Int, format: Int, maxImages: Int)
    private external fun nativeGetSurface(native: Long): Surface
    private external fun nativeAcquireLatestImage(native: Long): NativeImage?
    private external fun nativeAcquireNextImage(native: Long): NativeImage?
    private external fun nativeEnableImageAvailableListener(native: Long, enabled: Boolean)
    private external fun nativeClose(native: Long)
}
