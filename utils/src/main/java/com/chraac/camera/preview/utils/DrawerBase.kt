package com.chraac.camera.preview.utils

import android.opengl.Matrix
import android.util.Size
import androidx.annotation.WorkerThread

open class DrawerBase {

    var viewPortSize: Size = Size(0, 0)
        @WorkerThread
        set(value) {
            field = value
            Matrix.orthoM(projectionMatrix, 0, 0f, value.width.toFloat(),
                    value.height.toFloat(), 0f, 0f, 1f)
            projectionMatrixChanged = true
        }

    // 4x4
    protected var projectionMatrixChanged = true
        @WorkerThread get
        @WorkerThread set

    protected val projectionMatrix: FloatArray = FloatArray(MATRIX_SIZE).apply {
        Matrix.setIdentityM(this, 0)
    }

    protected var currentMatrixIndex = 0
    protected var matricesBuffer =
            FloatArray(INITIAL_RESTORE_STATE_SIZE * MATRIX_SIZE).apply {
                Matrix.setIdentityM(this, 0)
            }

    private val _tempMatrix: FloatArray = FloatArray(2 * MATRIX_SIZE)

    @WorkerThread
    fun save() {
        val nextMatrixIndex = currentMatrixIndex + MATRIX_SIZE
        if (nextMatrixIndex + MATRIX_SIZE > matricesBuffer.size) {
            matricesBuffer = matricesBuffer.copyOf(matricesBuffer.size * 2)
        }

        System.arraycopy(matricesBuffer, currentMatrixIndex,
                matricesBuffer, nextMatrixIndex, MATRIX_SIZE)
        currentMatrixIndex = nextMatrixIndex
    }

    @WorkerThread
    fun translate(x: Float, y: Float, z: Float) {
        Matrix.translateM(matricesBuffer, currentMatrixIndex, x, y, z)
    }

    @WorkerThread
    fun scale(sx: Float, sy: Float, sz: Float) {
        Matrix.scaleM(matricesBuffer, currentMatrixIndex, sx, sy, sz)
    }

    @WorkerThread
    fun rotate(angle: Float, x: Float, y: Float, z: Float) {
        if (angle == 0f) {
            return
        }

        val temp = _tempMatrix
        Matrix.setRotateM(temp, 0, angle, x, y, z)
        val matrix = matricesBuffer
        Matrix.multiplyMM(temp, MATRIX_SIZE, matrix, currentMatrixIndex, temp, 0)
        System.arraycopy(temp, MATRIX_SIZE, matrix, currentMatrixIndex, MATRIX_SIZE)
    }

    @WorkerThread
    fun restore() {
        check(currentMatrixIndex >= MATRIX_SIZE && currentMatrixIndex % MATRIX_SIZE == 0)
        currentMatrixIndex -= MATRIX_SIZE
    }

    companion object {
        // 4 x 4
        internal const val MATRIX_SIZE = 16
        internal const val INITIAL_RESTORE_STATE_SIZE = 8
        internal const val FLOAT_SIZE = java.lang.Float.SIZE / java.lang.Byte.SIZE
    }
}