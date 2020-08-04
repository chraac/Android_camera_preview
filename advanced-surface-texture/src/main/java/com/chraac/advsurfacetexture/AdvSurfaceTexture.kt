package com.chraac.advsurfacetexture

import android.annotation.TargetApi
import android.hardware.HardwareBuffer
import android.opengl.EGL14.EGL_NO_DISPLAY
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20.*
import android.opengl.GLU
import android.opengl.Matrix
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.annotation.*
import androidx.annotation.IntRange

private const val TEXTURE_TARGET = GL_TEXTURE_EXTERNAL_OES

private val IS_ANDROID_9_OR_ABOVE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

val IS_ADV_SURFACE_TEXTURE_AVAILABLE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

@TargetApi(Build.VERSION_CODES.O)
class AdvSurfaceTexture internal constructor(
        textureId: Int,
        glFunctions: GLFunctions,
        eglFunctions: EGLFunctions,
        imageReaderProvider: () -> ImageReader
) : ImageReader.OnImageAvailableListener, SurfaceTextureProvider {

    @Suppress("MemberVisibilityCanBePrivate", "MemberVisibilityCanBePrivate")
    val textureId
        get() = _glTextureId

    @GuardedBy("this")
    var errorHandler: ((Int) -> Unit)? = {
        synchronized(this) {
            val errorString = GLU.gluErrorString(it) ?: "$it"
            throw RuntimeException("glGetError: $errorString")
        }
    }

    override val timestamp: Long
        get() = _image?.timestamp ?: 0

    override val surface: Surface by lazy {
        _imageReader.surface
    }

    override val width: Int
        get() = _imageReader.width

    override val height: Int
        get() = _imageReader.height

    private var _image: ImageReader.Image? = null

    private var _glTextureId = 0

    private var _hardwareBuffer: HardwareBuffer? = null

    private var _eglImageKHR: EGLImageKHR? = null

    @GuardedBy("this")
    private var _handler: Handler? = null

    @GuardedBy("this")
    private var _surfaceTextureListener: SurfaceTextureProvider.OnFrameAvailableListener? = null

    private val _glFunctions: GLFunctions = glFunctions

    private val _eglFunctions: EGLFunctions = eglFunctions

    private val _imageReader: ImageReader by lazy {
        imageReaderProvider.invoke()
    }

    private val _transformMatrix = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
    }

    init {
        if (!IS_ADV_SURFACE_TEXTURE_AVAILABLE) {
            throw UnsupportedOperationException("AdvSurfaceTexture is unavailable in SDK_INT < 26")
        }

        if (textureId > 0) {
            attachToGLContext(textureId)
        }
    }

    constructor(@IntRange(from = 1) width: Int,
                @IntRange(from = 1) height: Int,
                format: Int,
                maxImages: Int,
                textureId: Int,
                glFunctions: GLFunctions) :
            this(textureId = textureId,
                    glFunctions = glFunctions,
                    eglFunctions = EGLFunctionsImpl,
                    imageReaderProvider = {
                        check(width > 0) { "Invalid width" }
                        check(height > 0) { "Invalid height" }
                        if (IS_ANDROID_9_OR_ABOVE) {
                            SystemImageReader(width, height, format, maxImages)
                        } else {
                            NativeImageReader(width, height, format, maxImages)
                        }
                    })

    @MainThread
    override fun setOnFrameAvailableListener(
            listener: SurfaceTextureProvider.OnFrameAvailableListener?, handler: Handler?) {
        if (listener != null) {
            _imageReader.setOnImageAvailableListener(this, handler)
        } else {
            _imageReader.setOnImageAvailableListener(null, null)
        }

        synchronized(this) {
            _surfaceTextureListener = listener
        }
    }

    @WorkerThread
    override fun getTransformMatrix(@Size(value = 16) mtx: FloatArray) {
        check(mtx.size != 16) { "Invalid matrix size" }
        _transformMatrix.copyInto(mtx)
    }

    @WorkerThread
    override fun updateTexImage() {
        check(_handler != null && Looper.myLooper() == _handler?.looper) { "Illegal update thread" }
        val textureId = if (_glTextureId != 0) _glTextureId else return
        checkGLError()
        releaseTexImage()
        try {
            /**
             * This operation will fail by throwing an {@link java.lang.IllegalStateException} if
             * {@code android.media.ImageReader.maxImages} have been acquired with
             * {@link android.media.ImageReader#acquireLatestImage} or
             * {@link android.media.ImageReader#acquireNextImage}.
             */
            val image = _imageReader.acquireLatestImage() ?: return
            _image = image
            val hardwareBuffer = image.hardwareBuffer
                    ?: throw IllegalStateException("Invalid HardwareBuffer")
            _hardwareBuffer = hardwareBuffer
            _eglImageKHR = _eglFunctions.eglCreateImageFromHardwareBuffer(EGL_NO_DISPLAY, hardwareBuffer)
                    ?: throw IllegalStateException("Invalid EGLImage")
            _glFunctions.glBindTexture(TEXTURE_TARGET, textureId)
            checkGLError()
            _eglFunctions.glEGLImageTargetTexture2DOES(TEXTURE_TARGET, _eglImageKHR)
            checkGLError()
        } catch (e: Throwable) {
            releaseTexImage()
            throw e
        }
    }

    @WorkerThread
    override fun releaseTexImage() {
        check(_handler == null || Looper.myLooper() == _handler?.looper) { "Illegal release thread" }
        _glFunctions.glActiveTexture(GL_TEXTURE0)
        _glFunctions.glBindTexture(TEXTURE_TARGET, 0)
        checkGLError()
        _eglFunctions.eglDestroyImageKHR(EGL_NO_DISPLAY, _eglImageKHR)
        _eglImageKHR = null
        _hardwareBuffer?.close()
        _hardwareBuffer = null
        _image?.close()
        _image = null
        _eglImageKHR = null
    }

    @WorkerThread
    override fun attachToGLContext(texName: Int) {
        detachFromGLContext()
        checkGLError()
        _glFunctions.glActiveTexture(GL_TEXTURE0)
        _glFunctions.glBindTexture(TEXTURE_TARGET, textureId)
        checkGLError()
        _glFunctions.glTexParameteri(TEXTURE_TARGET, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        _glFunctions.glTexParameteri(TEXTURE_TARGET, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        _glFunctions.glTexParameteri(TEXTURE_TARGET, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        _glFunctions.glTexParameteri(TEXTURE_TARGET, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        checkGLError()
        _glTextureId = texName
        val looper = Looper.myLooper()
        check(looper != null) { "Unable to get Looper" }
        _handler = Handler(looper)
    }

    @WorkerThread
    override fun detachFromGLContext() {
        check(_handler == null || Looper.myLooper() == _handler?.looper) { "Illegal detach thread" }
        releaseTexImage()
        _glTextureId = 0
    }

    @WorkerThread
    override fun close() = synchronized(this) {
        check(_handler != null && Looper.myLooper() == _handler?.looper) { "Illegal close thread" }
        _surfaceTextureListener = null
        detachFromGLContext()
        _imageReader.close()
        _handler = null
    }

    @WorkerThread
    override fun onImageAvailable(reader: ImageReader) {
        val listener = synchronized(this) {
            _surfaceTextureListener ?: return
        }

        listener.onFrameAvailable(this)
    }

    private fun checkGLError() = synchronized(this) {
        val handler = errorHandler ?: return@synchronized
        val error = _glFunctions.glGetError()
        if (error != GL_NO_ERROR) {
            handler.invoke(error)
        }
    }

}