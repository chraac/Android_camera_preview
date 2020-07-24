package com.chraac.advsurfacetexture

import android.hardware.HardwareBuffer
import android.os.Handler
import android.view.Surface

interface ImageReader : AutoCloseable {

    interface Image : AutoCloseable {
        val width: Int

        val height: Int

        val format: Int

        val timestamp: Long

        val hardwareBuffer: HardwareBuffer?
    }

    interface OnImageAvailableListener {
        /**
         * Callback that is called when a new image is available from ImageReader.
         *
         * @param reader the ImageReader the callback is associated with.
         * @see ImageReader
         * @see Image
         */
        fun onImageAvailable(reader: ImageReader)
    }

    val surface: Surface

    val width: Int

    val height: Int

    val format: Int

    val maxImages: Int

    fun setOnImageAvailableListener(listener: OnImageAvailableListener?, handler: Handler?)

    fun acquireLatestImage(): Image?

    fun acquireNextImage(): Image?
}