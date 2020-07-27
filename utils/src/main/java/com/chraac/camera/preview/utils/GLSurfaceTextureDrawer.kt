package com.chraac.camera.preview.utils

import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20.*
import android.opengl.GLU
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.chraac.camera.preview.utils.DrawerBase.Companion.FLOAT_SIZE
import java.nio.FloatBuffer

private const val INDEX_VERTEX_X = 0
private const val INDEX_VERTEX_Y = 1
private const val INDEX_TEXTURE_U = 2
private const val INDEX_TEXTURE_V = 3
private const val VERTEX_COMPONENT_COUNT = 2
private const val TEX_COORD_COMPONENT_COUNT = 2
private const val ATTRIBUTE_FLOAT_COUNT = VERTEX_COMPONENT_COUNT + TEX_COORD_COMPONENT_COUNT
private const val ATTRIBUTE_STRIDE = ATTRIBUTE_FLOAT_COUNT * FLOAT_SIZE

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
    tex_coord_frag = tex_coord;
}
"""

private const val FRAGMENT_SHADER_TEMPLATE = """
%s
precision highp float;
#define SamplerType %s
uniform SamplerType texture;
varying vec2 tex_coord_frag;

void main()
{
    gl_FragColor = texture2D(texture, tex_coord_frag);
}
"""

private val FRAGMENT_SHADER_TEX = FRAGMENT_SHADER_TEMPLATE.format(
        "", "sampler2D")

private val FRAGMENT_SHADER_OES = FRAGMENT_SHADER_TEMPLATE.format(
        "#extension GL_OES_EGL_image_external: require", "samplerExternalOES")

internal fun checkGLError() {
    val error = glGetError()
    if (error != GL_NO_ERROR) {
        val errorString = GLU.gluErrorString(error) ?: "$error"
        throw RuntimeException("glGetError: $errorString")
    }
}

private fun loadShader(type: Int, shaderSource: String): Int {
    val shader = glCreateShader(type)
    glShaderSource(shader, shaderSource)
    checkGLError()
    glCompileShader(shader)
    checkGLError()
    return shader
}

@WorkerThread
private class Program(
        vertexShader: String,
        fragmentShader: String
) : AutoCloseable {

    val program: Int
        get() = _program
    val uniformProjection: Int
        get() = _uniformProjection
    val uniformTransform: Int
        get() = _uniformTransform
    val uniformTexture: Int
        get() = _uniformTexture
    val attributePosition: Int
        get() = _attributePosition
    val attributeTexCoord: Int
        get() = _attributeTexCoord

    private var _program: Int = 0
    private var _uniformProjection: Int = 0
    private var _uniformTransform: Int = 0
    private var _uniformTexture: Int = 0
    private var _attributePosition: Int = 0
    private var _attributeTexCoord: Int = 0

    init {
        val program = glCreateProgram()
        checkGLError()

        val vertex = loadShader(GL_VERTEX_SHADER, vertexShader)
        glAttachShader(program, vertex)
        checkGLError()
        glDeleteShader(vertex)

        val fragment = loadShader(GL_FRAGMENT_SHADER, fragmentShader)
        glAttachShader(program, fragment)
        checkGLError()
        glDeleteShader(fragment)

        glLinkProgram(program)
        checkGLError()
        val linkStatus = IntArray(1)
        glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GL_TRUE) {
            val errorMessage = "Link gl program error: ${glGetProgramInfoLog(program)}"
            glDeleteProgram(program)
            throw RuntimeException(errorMessage)
        }

        // uniforms
        _uniformProjection = glGetUniformLocation(program, "projection")
        _uniformTransform = glGetUniformLocation(program, "transform")
        _uniformTexture = glGetUniformLocation(program, "texture")

        // attributes
        _attributePosition = glGetAttribLocation(program, "position")
        _attributeTexCoord = glGetAttribLocation(program, "tex_coord")

        _program = program
    }

    @WorkerThread
    override fun close() {
        if (_program != 0) {
            _uniformProjection = 0
            _uniformTransform = 0
            _uniformTexture = 0
            _attributePosition = 0
            _attributeTexCoord = 0
            glDeleteProgram(_program)
            _program = 0
        }
    }
}

@MainThread
class GLSurfaceTextureDrawer : DrawerBase(), SurfaceTextureDrawer, AutoCloseable {

    private var _verticesBufferId: Int = 0

    // 4 x (sizeof(vec2) + sizeof(vec2))
    private val _verticesBuffer = FloatBuffer.allocate(16).apply {
        val floatArray = this.array()
        floatArray[INDEX_TEXTURE_U] = 0.0f
        floatArray[INDEX_TEXTURE_V] = 0.0f

        floatArray[ATTRIBUTE_FLOAT_COUNT + INDEX_TEXTURE_U] = 0.0f
        floatArray[ATTRIBUTE_FLOAT_COUNT + INDEX_TEXTURE_V] = 1.0f

        floatArray[10] = 1.0f
        floatArray[11] = 1.0f

        floatArray[14] = 1.0f
        floatArray[15] = 0.0f
    }

    private val _programList = arrayListOf<Program?>(null, null)

    @WorkerThread
    override fun beginFrame() {
        glClearColor(0f, 0f, 0f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)
        super.beginFrame()
    }

    @WorkerThread
    override fun draw(surfaceTexture: SurfaceTextureManager) {
        beginDraw(surfaceTexture)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        endDraw(surfaceTexture)
    }

    @WorkerThread
    override fun close() {
        _programList.forEach {
            it?.close()
        }

        if (_verticesBufferId != 0) {
            glBindBuffer(GL_ARRAY_BUFFER, 0)
            glDeleteBuffers(1, intArrayOf(_verticesBufferId), 0)
        }
    }

    @WorkerThread
    private fun beginDraw(surfaceTexture: SurfaceTextureManager) {
        val texId = surfaceTexture.bind(this)
        if (texId == 0) {
            throw RuntimeException("SurfaceTextureExt.bind.failed")
        }

        val program = prepareProgram(surfaceTexture.target)
        getVertices(surfaceTexture, _verticesBuffer)
        prepareDrawParam(surfaceTexture.target, texId, program)
    }

    @WorkerThread
    private fun endDraw(surfaceTexture: SurfaceTextureManager) {
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(surfaceTexture.target, 0)
        surfaceTexture.unbind()
    }

    @WorkerThread
    private fun prepareProgram(target: Int): Program {
        require(target == GL_TEXTURE_EXTERNAL_OES || target == GL_TEXTURE_2D)

        val programIndex: Int
        val fragmentShader: String
        when (target) {
            GL_TEXTURE_2D -> {
                programIndex = 0
                fragmentShader = FRAGMENT_SHADER_TEX
            }
            else -> {
                programIndex = 1
                fragmentShader = FRAGMENT_SHADER_OES
            }
        }

        var program = _programList[programIndex]
        if (program == null) {
            program = Program(VERTEX_SHADER, fragmentShader)
            _programList[programIndex] = program
        }

        glUseProgram(program.program)
        return program
    }

    @WorkerThread
    private fun prepareDrawParam(target: Int, texId: Int, program: Program) {
        if (projectionMatrixChanged) {
            glUniformMatrix4fv(program.uniformProjection, 1, false,
                    projectionMatrix, 0)
            projectionMatrixChanged = false
            glViewport(0, 0, viewPortSize.width, viewPortSize.height)
            checkGLError()
        }

        glUniformMatrix4fv(program.uniformTransform, 1, false,
                matricesBuffer, currentMatrixIndex)
        checkGLError()

        // texture
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(target, texId)
        glUniform1i(program.uniformTexture, 0)
        checkGLError()

        // vertices buffer
        if (_verticesBufferId == 0) {
            val verticesBuffer = IntArray(1)
            glGenBuffers(verticesBuffer.size, verticesBuffer, 0)
            _verticesBufferId = verticesBuffer[0]
            checkGLError()
        }

        glBindBuffer(GL_ARRAY_BUFFER, _verticesBufferId)
        glBufferData(GL_ARRAY_BUFFER,
                _verticesBuffer.capacity() * FLOAT_SIZE, _verticesBuffer, GL_STREAM_DRAW)

        glEnableVertexAttribArray(program.attributePosition)
        glVertexAttribPointer(program.attributePosition,
                VERTEX_COMPONENT_COUNT,
                GL_FLOAT,
                false,
                ATTRIBUTE_STRIDE,
                0)

        glEnableVertexAttribArray(program.attributeTexCoord)
        glVertexAttribPointer(program.attributeTexCoord,
                TEX_COORD_COMPONENT_COUNT,
                GL_FLOAT,
                false,
                ATTRIBUTE_STRIDE,
                VERTEX_COMPONENT_COUNT * FLOAT_SIZE)
    }

    companion object {

        fun getVertices(surfaceTexture: SurfaceTextureManager, verticesBuffer: FloatBuffer) {
            val floatArray = verticesBuffer.array()
            val width = surfaceTexture.width
            val height = surfaceTexture.height
            var vertexOffset = 0
            floatArray[vertexOffset + INDEX_VERTEX_X] = 0.0f
            floatArray[vertexOffset + INDEX_VERTEX_Y] = 0.0f
            floatArray[vertexOffset + INDEX_TEXTURE_U] = 0.0f
            floatArray[vertexOffset + INDEX_TEXTURE_V] = 0.0f

            vertexOffset += ATTRIBUTE_FLOAT_COUNT
            floatArray[vertexOffset + INDEX_VERTEX_X] = width.toFloat()
            floatArray[vertexOffset + INDEX_VERTEX_Y] = 0.0f
            floatArray[vertexOffset + INDEX_TEXTURE_U] = 1.0f
            floatArray[vertexOffset + INDEX_TEXTURE_V] = 0.0f

            vertexOffset += ATTRIBUTE_FLOAT_COUNT
            floatArray[vertexOffset + INDEX_VERTEX_X] = 0.0f
            floatArray[vertexOffset + INDEX_VERTEX_Y] = height.toFloat()
            floatArray[vertexOffset + INDEX_TEXTURE_U] = 0.0f
            floatArray[vertexOffset + INDEX_TEXTURE_V] = 1.0f

            vertexOffset += ATTRIBUTE_FLOAT_COUNT
            floatArray[vertexOffset + INDEX_VERTEX_X] = width.toFloat()
            floatArray[vertexOffset + INDEX_VERTEX_Y] = height.toFloat()
            floatArray[vertexOffset + INDEX_TEXTURE_U] = 1.0f
            floatArray[vertexOffset + INDEX_TEXTURE_V] = 1.0f
        }

    }
}