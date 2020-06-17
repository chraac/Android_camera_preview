package com.example.android.camera.utils

import android.graphics.SurfaceTexture
import android.opengl.Matrix
import android.util.Size
import androidx.annotation.WorkerThread

private const val VERTEX_SHADER = """
precision highp float;
attribute vec2 position;
attribute vec2 tex_coord;
uniform mat4 projection;
uniform mat4 transform;
varying vec2 tex_coord_frag;

void main()
{
    /*
    * ret = position * transform * projection
    * note that:
    * matrix operations is column-major in spec
    */
    mat4 mp = projection * transform;
    gl_Position = mp * vec4(position, 0.0, 1.0);
    tex_coord_out = tex_coord;
}
"""

private const val FRAGMENT_SHADER = """
precision highp float;
uniform sampler2D texture;
varying vec2 tex_coord_frag;

void main()
{
    gl_FragColor = texture2D(texture, tex_coord_frag);
}
"""

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

    private val _projectionMatrix: FloatArray = FloatArray(64) // 4x4
    private var _projectionMatrixChanged = true
    private val _transformMatrix: FloatArray = FloatArray(16) // 4x4
    private var _transformMatrixChanged = true
    private val _matrixBuffer: FloatArray = FloatArray(16) // 4x4

    // 4 x (sizeof(vec2) + sizeof(vec2))
    private val _verticesBuffer: FloatArray = FloatArray(16)

    override fun draw(surfaceTexture: SurfaceTexture) {
        if (_projectionMatrixChanged) {

            _projectionMatrixChanged = false
        }
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}