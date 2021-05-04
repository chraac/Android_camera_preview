package com.chraac.advsurfacetexture

import android.graphics.SurfaceTexture
import android.media.Image
import android.os.Build
import android.os.Handler
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.inOrder
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.media.ImageReader as SystemImageReader
import android.media.ImageWriter as SystemImageWriter


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class AdvSurfaceTextureExUnitTest {

    private val mockSurfaceTexture = mock<SurfaceTexture>()
    private val mockImage = mock<Image>()
    private val mockImageReader = mock<SystemImageReader>().apply {
        doReturn(mockImage).whenever(this).acquireLatestImage()
        doReturn(mockImage).whenever(this).acquireNextImage()
    }
    private val mockImageWriter = mock<SystemImageWriter>()

    private val subject = spy(
        AdvSurfaceTextureEx(
            surfaceTexture = mockSurfaceTexture,
            imageReaderProvider = { mockImageReader },
            imageWriteProvider = { mockImageWriter }
        )
    )

    @Test
    fun `attachToGLContext will call SurfaceTexture#attachToGLContext`() {
        subject.attachToGLContext(1)
        verify(mockSurfaceTexture).attachToGLContext(eq(1))
    }

    @Test
    fun `detachFromGLContext will call SurfaceTexture#detachFromGLContext`() {
        subject.detachFromGLContext()
        verify(mockSurfaceTexture).detachFromGLContext()
    }

    @Test
    fun `getTransformMatrix will call SurfaceTexture#getTransformMatrix`() {
        val mtx = FloatArray(16)
        subject.getTransformMatrix(mtx)
        verify(mockSurfaceTexture).getTransformMatrix(eq(mtx))
    }

    @Test
    fun `releaseTexImage will call SurfaceTexture#releaseTexImage`() {
        subject.releaseTexImage()
        verify(mockSurfaceTexture).releaseTexImage()
    }

    @Test
    fun `updateTexImage will call SurfaceTexture#updateTexImage`() {
        subject.updateTexImage()
        verify(mockSurfaceTexture).updateTexImage()
    }

    @Test
    fun `setOnFrameAvailableListener will set listener to both image reader and surface texture`() {
        val mockListener = mock<SurfaceTextureProvider.OnFrameAvailableListener>()
        val mockHandler = mock<Handler>()
        subject.setOnFrameAvailableListener(mockListener, mockHandler)
        verify(mockImageReader).setOnImageAvailableListener(subject, mockHandler)
        verify(mockSurfaceTexture).setOnFrameAvailableListener(subject, mockHandler)
    }

    @Test
    fun `onImageAvailable will enqueue then close image`() {
        subject.onImageAvailable(mockImageReader)
        val inOrder = inOrder(mockImageReader, mockImageWriter, mockImage)
        inOrder.verify(mockImageReader).acquireLatestImage()
        inOrder.verify(mockImageWriter).queueInputImage(eq(mockImage))
        inOrder.verify(mockImage).close()
    }

    @Test
    fun `close will release all resources`() {
        subject.close()
        verify(mockSurfaceTexture).release()
        verify(mockImageReader).close()
        verify(mockImageWriter).close()
    }

}