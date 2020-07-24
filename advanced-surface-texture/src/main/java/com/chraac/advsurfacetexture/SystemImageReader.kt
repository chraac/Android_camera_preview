package com.chraac.advsurfacetexture

import android.annotation.TargetApi
import android.hardware.HardwareBuffer
import android.media.Image
import android.os.Handler
import android.view.Surface
import androidx.annotation.WorkerThread

@TargetApi(28)
class SystemImageReader constructor(
        override val width: Int,
        override val height: Int,
        override val format: Int,
        override val maxImages: Int
) : ImageReader, android.media.ImageReader.OnImageAvailableListener {

    private class SystemImage constructor(
            private val _image: Image
    ) : ImageReader.Image {
        override val width: Int
            get() = _image.width

        override val height: Int
            get() = _image.height

        override val format: Int
            get() = _image.format

        override val timestamp: Long
            get() = _image.timestamp

        override val hardwareBuffer: HardwareBuffer?
            get() = _image.hardwareBuffer

        override fun close() = _image.close()
    }

    override val surface: Surface by lazy {
        _imageReader.surface
    }

    private val _imageReader =
            android.media.ImageReader.newInstance(width, height, format, maxImages)

    private var _listener: ImageReader.OnImageAvailableListener? = null

    override fun acquireLatestImage(): ImageReader.Image? =
            SystemImage(_imageReader.acquireLatestImage())

    override fun acquireNextImage(): ImageReader.Image? =
            SystemImage(_imageReader.acquireNextImage())

    override fun setOnImageAvailableListener(listener: ImageReader.OnImageAvailableListener?, handler: Handler?) {
        _listener = listener
        if (listener != null) {
            _imageReader.setOnImageAvailableListener(this, handler)
        } else {
            _imageReader.setOnImageAvailableListener(null, null)
        }
    }

    override fun close() = _imageReader.close()

    @WorkerThread
    override fun onImageAvailable(reader: android.media.ImageReader?) {
        _listener?.onImageAvailable(this)
    }
}