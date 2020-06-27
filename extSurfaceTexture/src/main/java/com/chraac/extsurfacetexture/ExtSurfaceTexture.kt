package com.chraac.extsurfacetexture

import android.annotation.TargetApi
import android.media.Image
import android.media.ImageReader
import android.opengl.EGL14.EGL_NO_DISPLAY
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20.*
import android.os.Handler
import android.util.Size
import android.view.Surface
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread

private const val TEXTURE_TARGET = GL_TEXTURE_EXTERNAL_OES

@TargetApi(28)
class ExtSurfaceTexture constructor(
        width: Int,
        height: Int,
        val format: Int,
        val maxImages: Int,
        texName: Int
) : ImageReader.OnImageAvailableListener, SurfaceTextureProvider {

    val imageReader: ImageReader = ImageReader.newInstance(width, height, format, maxImages)

    val textureId
        get() = _glTextureId

    override val timestamp: Long
        get() = _image?.timestamp ?: 0

    override val surface: Surface by lazy {
        imageReader.surface
    }

    override val size: Size
        get() = Size(imageReader.width, imageReader.height)

    private var _image: Image? = null

    private var _glTextureId = 0

    private var _eglImage: EGLImage? = null

    private var _surfaceTextureListener: SurfaceTextureProvider.OnFrameAvailableListener? = null

    init {
        attachToGLContext(texName)
    }

    @MainThread
    override fun setOnFrameAvailableListener(
            listener: SurfaceTextureProvider.OnFrameAvailableListener?, handler: Handler?) {
        if (listener != null) {
            imageReader.setOnImageAvailableListener(this, handler)
        } else {
            imageReader.setOnImageAvailableListener(null, null)
        }

        _surfaceTextureListener = listener
    }

    @WorkerThread
    override fun updateTexImage() {
        val textureId = if (_glTextureId != 0) _glTextureId else return
        val image = _image ?: return
        EGLFunctions.eglDestroyImageKHR(EGL_NO_DISPLAY, _eglImage)
        _eglImage = EGLFunctions.eglCreateImageFromHardwareBuffer(EGL_NO_DISPLAY, image.hardwareBuffer)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(TEXTURE_TARGET, textureId)
        EGLFunctions.eglImageTargetTexture2DOES(TEXTURE_TARGET, _eglImage)
    }

    @WorkerThread
    override fun releaseTexImage() {
        EGLFunctions.eglDestroyImageKHR(EGL_NO_DISPLAY, _eglImage)
        _eglImage = null
    }

    @WorkerThread
    override fun attachToGLContext(texName: Int) {
        releaseTexImage()
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(TEXTURE_TARGET, textureId)
        glTexParameteri(TEXTURE_TARGET, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(TEXTURE_TARGET, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(TEXTURE_TARGET, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(TEXTURE_TARGET, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        _glTextureId = texName
    }

    @WorkerThread
    override fun detachFromGLContext() {
        releaseTexImage()
        _glTextureId = 0
    }

    @WorkerThread
    override fun close() = synchronized(this) {
        _surfaceTextureListener = null
        detachFromGLContext()
        _image?.close()
        imageReader.close()
        return@synchronized
    }

    @WorkerThread
    override fun onImageAvailable(reader: ImageReader?) {
        val listener: SurfaceTextureProvider.OnFrameAvailableListener
        synchronized(this) {
            listener = _surfaceTextureListener ?: return
            _image?.close()
            _image = try {
                imageReader.acquireLatestImage() ?: return
            } catch (e: IllegalStateException) {
                /**
                 * This operation will fail by throwing an {@link java.lang.IllegalStateException} if
                 * {@code android.media.ImageReader.maxImages} have been acquired with
                 * {@link android.media.ImageReader#acquireLatestImage} or
                 * {@link android.media.ImageReader#acquireNextImage}.
                 */
                return
            }
        }

        listener.onFrameAvailable(this)
    }

}