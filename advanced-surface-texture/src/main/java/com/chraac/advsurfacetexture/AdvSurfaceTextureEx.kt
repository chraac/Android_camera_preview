package com.chraac.advsurfacetexture

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.graphics.SurfaceTexture
import android.media.ImageReader as SystemImageReader
import android.media.ImageWriter as SystemImageWriter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread

val IS_ADV_SURFACE_TEXTURE_AVAILABLE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
@TargetApi(Build.VERSION_CODES.O)
class AdvSurfaceTextureEx(
    private val surfaceTexture: SurfaceTexture,
    imageReaderProvider: () -> SystemImageReader,
    imageWriteProvider: (surfaceTexture: SurfaceTexture) -> SystemImageWriter
) : SystemImageReader.OnImageAvailableListener,
    SurfaceTexture.OnFrameAvailableListener,
    SurfaceTextureProvider {

    override val timestamp: Long
        get() = surfaceTexture.timestamp

    override val surface: Surface by lazy {
        imageReader.surface
    }

    override val width: Int
        get() = imageReader.width

    override val height: Int
        get() = imageReader.height

    private var looper: Looper? = Looper.myLooper()

    @GuardedBy("this")
    private var surfaceTextureListener: SurfaceTextureProvider.OnFrameAvailableListener? = null

    private val imageReader by lazy {
        imageReaderProvider.invoke()
    }

    private val imageWriter by lazy {
        imageWriteProvider.invoke(surfaceTexture)
    }

    @SuppressLint("Recycle")
    constructor(
        @IntRange(from = 1) width: Int,
        @IntRange(from = 1) height: Int,
        format: Int,
        maxImages: Int,
        textureId: Int
    ) :
        this(
            surfaceTexture = when {
                textureId > 0 -> SurfaceTexture(textureId)
                else -> SurfaceTexture(false)
            },
            imageReaderProvider = {
                check(width > 0) { "Invalid width" }
                check(height > 0) { "Invalid height" }
                SystemImageReader.newInstance(width, height, format, maxImages)
            },
            imageWriteProvider = { surfaceTexture ->
                val surface = Surface(surfaceTexture)
                surface.setBuffersGeometry(width, height, format)
                SystemImageWriter.newInstance(surface, maxImages)
            }
        )

    @MainThread
    override fun setOnFrameAvailableListener(
        listener: SurfaceTextureProvider.OnFrameAvailableListener?, handler: Handler?
    ) {
        if (listener != null) {
            imageReader.setOnImageAvailableListener(this, handler)
            surfaceTexture.setOnFrameAvailableListener(this, handler)
        } else {
            imageReader.setOnImageAvailableListener(null, null)
            surfaceTexture.setOnFrameAvailableListener(null, null)
        }

        synchronized(this) {
            surfaceTextureListener = listener
        }
    }

    @WorkerThread
    override fun onImageAvailable(reader: SystemImageReader) {
        imageReader.acquireLatestImage()?.let { image ->
            imageWriter.queueInputImage(image)
            image.close()
        }
    }

    @WorkerThread
    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        synchronized(this) {
            surfaceTextureListener
        }?.onFrameAvailable(this)
    }

    override fun attachToGLContext(texName: Int) {
        Looper.myLooper().let {
            check(it != null) { "Unable to get Looper" }
            looper = it
        }
        surfaceTexture.attachToGLContext(texName)
    }

    override fun detachFromGLContext() {
        check(looper == null || Looper.myLooper() == looper) { "Illegal detach thread" }
        surfaceTexture.detachFromGLContext()
    }

    override fun getTransformMatrix(mtx: FloatArray) =
        surfaceTexture.getTransformMatrix(mtx)

    override fun releaseTexImage() {
        check(looper == null || Looper.myLooper() == looper) { "Illegal release thread" }
        surfaceTexture.releaseTexImage()
    }

    override fun updateTexImage() {
        check(looper == null || Looper.myLooper() == looper) { "Illegal update thread" }
        surfaceTexture.updateTexImage()
    }

    override fun close() {
        check(looper == null || Looper.myLooper() == looper) { "Illegal close thread" }
        surfaceTexture.release()
        imageWriter.close()
        imageReader.close()
        looper = null
    }
}