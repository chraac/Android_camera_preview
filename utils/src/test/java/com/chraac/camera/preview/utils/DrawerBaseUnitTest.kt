package com.chraac.camera.preview.utils

import android.os.Build
import android.util.Size
import com.chraac.camera.preview.utils.DrawerBase.Companion.INITIAL_RESTORE_STATE_SIZE
import com.chraac.camera.preview.utils.DrawerBase.Companion.MATRIX_SIZE
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class DrawerBaseUnitTest {

    class DummyDrawer : DrawerBase() {
        var openProjectionMatrixChanged
            get() = projectionMatrixChanged
            set(value) {
                projectionMatrixChanged = value
            }

        val openCurrentMatrixIndex get() = currentMatrixIndex
        val openMatricesBuffer get() = matricesBuffer
    }

    private val testObject = DummyDrawer()

    @Test
    fun `viewPortSize will change projection matrix`() {
        assertEquals(0, testObject.viewPortSize.width)
        assertEquals(0, testObject.viewPortSize.height)
        testObject.openProjectionMatrixChanged = false

        testObject.viewPortSize = Size(1920, 1080)
        assertEquals(true, testObject.openProjectionMatrixChanged)
    }

    @Test
    fun `save will change index`() {
        val matrixIndex = testObject.openCurrentMatrixIndex
        testObject.save()
        assertEquals(matrixIndex + MATRIX_SIZE, testObject.openCurrentMatrixIndex)
        testObject.restore()
        assertEquals(matrixIndex, testObject.openCurrentMatrixIndex)
    }

    @Test
    fun `save will copy matrix into new position`() {
        testObject.openMatricesBuffer[testObject.openCurrentMatrixIndex] = 2f
        testObject.openMatricesBuffer[testObject.openCurrentMatrixIndex + 4] = 3f
        testObject.openMatricesBuffer[testObject.openCurrentMatrixIndex + 7] = 4f
        testObject.openMatricesBuffer[testObject.openCurrentMatrixIndex + 8] = 5f
        testObject.openMatricesBuffer[testObject.openCurrentMatrixIndex + 9] = 6f
        val previousMatrix = testObject.openMatricesBuffer.copyOfRange(
                testObject.openCurrentMatrixIndex, testObject.openCurrentMatrixIndex + MATRIX_SIZE)
        testObject.save()
        val newMatrix = testObject.openMatricesBuffer.copyOfRange(
                testObject.openCurrentMatrixIndex, testObject.openCurrentMatrixIndex + MATRIX_SIZE)
        assertEquals(true, previousMatrix.contentEquals(newMatrix))
        testObject.restore()
    }

    @Test
    fun `save will expand matrix buffer`() {
        val end = INITIAL_RESTORE_STATE_SIZE + 10
        val initIndex = testObject.openCurrentMatrixIndex
        for (i in 0 until end) {
            testObject.save()
        }

        assertEquals(initIndex + end * MATRIX_SIZE, testObject.openCurrentMatrixIndex)
        for (i in 0 until end) {
            testObject.restore()
        }
    }

    @Test
    fun `restore will restore changed matrix`() {
        val matrixIndex = testObject.openCurrentMatrixIndex
        val previousMatrix = testObject.openMatricesBuffer.copyOfRange(
                testObject.openCurrentMatrixIndex, testObject.openCurrentMatrixIndex + MATRIX_SIZE)
        testObject.save()
        testObject.openMatricesBuffer[testObject.openCurrentMatrixIndex] = 9f
        testObject.openMatricesBuffer[testObject.openCurrentMatrixIndex + 4] = 8f
        testObject.openMatricesBuffer[testObject.openCurrentMatrixIndex + 7] = 7f
        testObject.openMatricesBuffer[testObject.openCurrentMatrixIndex + 8] = 6f
        testObject.openMatricesBuffer[testObject.openCurrentMatrixIndex + 9] = 5f
        val newMatrix = testObject.openMatricesBuffer.copyOfRange(
                testObject.openCurrentMatrixIndex, testObject.openCurrentMatrixIndex + MATRIX_SIZE)
        assertEquals(false, previousMatrix.contentEquals(newMatrix))

        testObject.restore()
        val restoredMatrix = testObject.openMatricesBuffer.copyOfRange(
                testObject.openCurrentMatrixIndex, testObject.openCurrentMatrixIndex + MATRIX_SIZE)
        assertEquals(true, previousMatrix.contentEquals(restoredMatrix))
        assertEquals(matrixIndex, testObject.openCurrentMatrixIndex)
    }

}