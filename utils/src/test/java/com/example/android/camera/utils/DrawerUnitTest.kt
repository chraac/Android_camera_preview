package com.example.android.camera.utils

import android.util.Size
import com.example.android.camera.utils.DrawerBase.Companion.INITIAL_RESTORE_STATE_SIZE
import com.example.android.camera.utils.DrawerBase.Companion.MATRIX_SIZE
import junit.framework.TestCase.assertEquals
import org.junit.Test

class DrawerUnitTest {

    class DummyDrawer : DrawerBase() {
        var openProjectionMatrixChanged
            get() = projectionMatrixChanged
            set(value) {
                projectionMatrixChanged = value
            }

        val openProjectionMatrix get() = projectionMatrix
        val openCurrentMatrixIndex get() = currentMatrixIndex
        val openMatricesBuffer get() = matricesBuffer
    }

    private val testObject = DummyDrawer()

    @Test
    fun viewPortSize_projectionMatrixChange() {
        assertEquals(0, testObject.viewPortSize.width)
        assertEquals(0, testObject.viewPortSize.height)
        testObject.openProjectionMatrixChanged = false

        testObject.viewPortSize = Size(1920, 1080)
        assertEquals(1920, testObject.viewPortSize.width)
        assertEquals(1080, testObject.viewPortSize.height)
        assertEquals(true, testObject.openProjectionMatrixChanged)
        assertEquals(2f / 1920f, testObject.openProjectionMatrix[0])
        assertEquals(-2f / 1080f, testObject.openProjectionMatrix[5])
    }

    @Test
    fun save_indexChangeAfterSave() {
        val matrixIndex = testObject.openCurrentMatrixIndex
        testObject.save()
        assertEquals(matrixIndex + MATRIX_SIZE, testObject.openCurrentMatrixIndex)
        testObject.restore()
        assertEquals(matrixIndex, testObject.openCurrentMatrixIndex)
    }

    @Test
    fun save_copyMatrixIntoNewPosition() {
        testObject.translate(1f, 2f, 3f)
        testObject.scale(4f, 5f, 6f)
        testObject.rotate(180f, 7f, 8f, 9f)
        val previousMatrix = testObject.openMatricesBuffer.copyOfRange(
                testObject.openCurrentMatrixIndex, testObject.openCurrentMatrixIndex + MATRIX_SIZE)
        testObject.save()
        val newMatrix = testObject.openMatricesBuffer.copyOfRange(
                testObject.openCurrentMatrixIndex, testObject.openCurrentMatrixIndex + MATRIX_SIZE)
        assertEquals(true, previousMatrix.contentEquals(newMatrix))
        testObject.restore()
    }

    @Test
    fun save_matrixBufferExpand() {
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
    fun restore_restoreMatrixAfterChange() {
        val matrixIndex = testObject.openCurrentMatrixIndex
        val previousMatrix = testObject.openMatricesBuffer.copyOfRange(
                testObject.openCurrentMatrixIndex, testObject.openCurrentMatrixIndex + MATRIX_SIZE)
        testObject.save()
        testObject.translate(9f, 8f, 7f)
        testObject.scale(6f, 5f, 4f)
        testObject.rotate(90f, 3f, 2f, 1f)
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