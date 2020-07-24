package com.chraac.advsurfacetexture

import android.hardware.HardwareBuffer
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.os.Build
import com.nhaarman.mockitokotlin2.*
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class AdvSurfaceTextureUnitTest {

    private val glFunctions = mock<GLFunctions>()

    private val eglFunctions = mock<EGLFunctions>()

    private val imageReader = mock<ImageReader>()

    private val image = mock<ImageReader.Image>()

    private val hardwareBuffer = mock<HardwareBuffer>()

    private val eglImage = mock<EGLImageKHR>()

    @Before
    fun setup() {
        doReturn(image).`when`(imageReader).acquireLatestImage()
        doReturn(image).`when`(imageReader).acquireNextImage()
        doReturn(hardwareBuffer).`when`(image).hardwareBuffer
        doReturn(eglImage).`when`(eglFunctions)
                .eglCreateImageFromHardwareBuffer(anyOrNull(), eq(hardwareBuffer))
    }

    @Test
    fun `updateTexImage will release resources when eglImageKHR is null`() =
            runWithNewObject { subject ->
                subject.attachToGLContext(1)
                subject.updateTexImage()
                clearInvocations(glFunctions)
                clearInvocations(eglFunctions)
                doReturn(null).`when`(eglFunctions)
                        .eglCreateImageFromHardwareBuffer(anyOrNull(), anyOrNull())
                var isExceptionThrown = false
                try {
                    subject.updateTexImage()
                } catch (e: IllegalStateException) {
                    isExceptionThrown = true
                }

                assertEquals(true, isExceptionThrown)
                verify(glFunctions, times(2)).glActiveTexture(any())
                verify(glFunctions, times(2))
                        .glBindTexture(eq(GL_TEXTURE_EXTERNAL_OES), eq(0))
                verify(eglFunctions, times(1))
                        .eglDestroyImageKHR(anyOrNull(), eq(eglImage))
                verify(hardwareBuffer, times(2)).close()
                verify(image, times(2)).close()
                verify(imageReader, never()).close()
                verify(eglFunctions, never()).glEGLImageTargetTexture2DOES(anyOrNull(), anyOrNull())
            }

    @Test
    fun `updateTexImage will release resources when hardwareBuffer is null`() =
            runWithNewObject { subject ->
                subject.attachToGLContext(1)
                subject.updateTexImage()
                clearInvocations(glFunctions)
                clearInvocations(eglFunctions)
                doReturn(null).`when`(image).hardwareBuffer
                var isExceptionThrown = false
                try {
                    subject.updateTexImage()
                } catch (e: IllegalStateException) {
                    isExceptionThrown = true
                }

                assertEquals(true, isExceptionThrown)
                verify(glFunctions, times(2)).glActiveTexture(any())
                verify(glFunctions, times(2))
                        .glBindTexture(eq(GL_TEXTURE_EXTERNAL_OES), eq(0))
                verify(eglFunctions, times(1))
                        .eglDestroyImageKHR(anyOrNull(), eq(eglImage))
                verify(hardwareBuffer, times(1)).close()
                verify(image, times(2)).close()
                verify(imageReader, never()).close()
                verify(eglFunctions, never()).glEGLImageTargetTexture2DOES(anyOrNull(), anyOrNull())
            }

    @Test
    fun `updateTexImage will release resources when ImageReader throws ISE`() =
            runWithNewObject { subject ->
                subject.attachToGLContext(1)
                subject.updateTexImage()
                clearInvocations(glFunctions)
                clearInvocations(eglFunctions)
                doThrow(IllegalStateException("ISE for testing"))
                        .`when`(imageReader).acquireNextImage()
                doThrow(IllegalStateException("ISE for testing"))
                        .`when`(imageReader).acquireLatestImage()
                var isExceptionThrown = false
                try {
                    subject.updateTexImage()
                } catch (e: IllegalStateException) {
                    isExceptionThrown = true
                }

                assertEquals(true, isExceptionThrown)
                verify(glFunctions, times(2)).glActiveTexture(any())
                verify(glFunctions, times(2))
                        .glBindTexture(eq(GL_TEXTURE_EXTERNAL_OES), eq(0))
                verify(eglFunctions, times(1))
                        .eglDestroyImageKHR(anyOrNull(), eq(eglImage))
                verify(hardwareBuffer, times(1)).close()
                verify(image, times(1)).close()
                verify(imageReader, never()).close()
                verify(eglFunctions, never()).glEGLImageTargetTexture2DOES(anyOrNull(), anyOrNull())
            }

    @Test
    fun `updateTexImage will release previous update resources`() = runWithNewObject { subject ->
        subject.attachToGLContext(1)
        subject.updateTexImage()
        clearInvocations(glFunctions)
        clearInvocations(eglFunctions)
        subject.updateTexImage()
        verify(glFunctions, times(1)).glActiveTexture(any())
        verify(glFunctions, times(1))
                .glBindTexture(eq(GL_TEXTURE_EXTERNAL_OES), eq(0))
        verify(eglFunctions, times(1)).eglDestroyImageKHR(anyOrNull(), eq(eglImage))
        verify(hardwareBuffer, times(1)).close()
        verify(image, times(1)).close()
        verify(imageReader, never()).close()
        verify(glFunctions, times(1))
                .glBindTexture(eq(GL_TEXTURE_EXTERNAL_OES), eq(1))
        verify(eglFunctions, times(1))
                .glEGLImageTargetTexture2DOES(eq(GL_TEXTURE_EXTERNAL_OES), eq(eglImage))
    }

    @Test
    fun `releaseTexImage will release resources except ImageReader`() = runWithNewObject { subject ->
        subject.attachToGLContext(1)
        subject.updateTexImage()
        clearInvocations(glFunctions)
        subject.releaseTexImage()
        verify(glFunctions, times(1)).glActiveTexture(any())
        verify(glFunctions, times(1))
                .glBindTexture(eq(GL_TEXTURE_EXTERNAL_OES), eq(0))
        verify(eglFunctions, times(1)).eglDestroyImageKHR(anyOrNull(), eq(eglImage))
        verify(hardwareBuffer, times(1)).close()
        verify(image, times(1)).close()
        verify(imageReader, never()).close()
    }

    @Test
    fun `close will release all resources`() = runWithNewObject { subject ->
        subject.attachToGLContext(1)
        subject.updateTexImage()
        clearInvocations(glFunctions)
        subject.close()
        verify(eglFunctions, times(1)).eglDestroyImageKHR(anyOrNull(), eq(eglImage))
        verify(hardwareBuffer, times(1)).close()
        verify(image, times(1)).close()
        verify(imageReader, times(1)).close()
    }

    private fun runWithNewObject(block: (AdvSurfaceTexture) -> Unit) {
        block.invoke(spy(AdvSurfaceTexture(
                textureId = 0,
                glFunctions = glFunctions,
                eglFunctions = eglFunctions,
                imageReader = imageReader
        )))
    }
}