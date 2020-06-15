package com.example.android.camera.utils

import android.graphics.SurfaceTexture
import android.opengl.Matrix
import android.util.Size
import androidx.annotation.WorkerThread

class GLSurfaceTextureDrawer : SurfaceTextureDrawer, AutoCloseable {

    init {
    }

    override var viewPortSize: Size = Size(0, 0)
        @WorkerThread
        set(value) {
            field = value
            Matrix.orthoM(_projectionMatrix, 0, 0f, value.width.toFloat(),
                    0f, value.height.toFloat(), 0f, 1f)
        }

    private val _projectionMatrix: FloatArray = FloatArray(16) // 4x4
    private var _projectionMatrixChanged = true
    private val _transformMatrix: FloatArray = FloatArray(16) // 4x4
    private var _transformMatrixChanged = true
    private val _tempMatrix: FloatArray = FloatArray(16) // 4x4

    override fun draw(surfaceTexture: SurfaceTexture) {
        if (_projectionMatrixChanged) {

            _projectionMatrixChanged = false
        }
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}