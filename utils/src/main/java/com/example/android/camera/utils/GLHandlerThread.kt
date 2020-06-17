package com.example.android.camera.utils

import android.opengl.*
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import javax.microedition.khronos.egl.EGL10

val CONFIG_RGB = intArrayOf(
        EGL10.EGL_RED_SIZE, 8,
        EGL10.EGL_GREEN_SIZE, 8,
        EGL10.EGL_BLUE_SIZE, 8,
        EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
        EGL10.EGL_NONE
)

var CONTEXT_ATTR = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)

class GLHandlerThread @MainThread constructor(name: String, surface: Surface) : HandlerThread(name) {

    private val handler: Handler by lazy {
        Handler(looper)
    }

    private var _eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var _eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var _eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var _eglConfig: EGLConfig? = null
    private val _windowsSurface: Surface = surface
    private var _surfaceWidth: Int = 0
    private var _surfaceHeight: Int = 0

    init {
        start()
    }

    @WorkerThread
    override fun onLooperPrepared() {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) {
            throw UnsupportedOperationException("Unable to get egl display")
        }

        val ver = IntArray(2)
        EGL14.eglInitialize(display, ver, 0, ver, 1)

        val configs: Array<EGLConfig?> = arrayOfNulls(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(display, CONFIG_RGB, 0, configs, 0,
                        configs.size, numConfigs, 0) || numConfigs[0] == 0) {
            throw UnsupportedOperationException("Unable to choose egl config")
        }

        val context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT,
                CONTEXT_ATTR, 0)
        if (context == EGL14.EGL_NO_CONTEXT) {
            EGL14.eglTerminate(display)
            throw UnsupportedOperationException("Unable to create egl context")
        }

        _eglContext = context
        _eglDisplay = display
        _eglConfig = configs[0]
    }

    @WorkerThread
    fun swapBuffer(): Int {
        if (_eglDisplay == EGL14.EGL_NO_DISPLAY || _eglSurface == EGL14.EGL_NO_SURFACE) {
            return EGL14.EGL_BAD_DISPLAY
        }

        return if (EGL14.eglSwapBuffers(_eglDisplay, _eglSurface)) EGL14.EGL_SUCCESS else EGL14.eglGetError()
    }

    @MainThread
    fun notifySurfaceSizeChange(width: Int, height: Int) {
        if (!isAlive) {
            return
        }

        handler.post {
            recreateSurface(width, height)
        }
    }

    @MainThread
    fun destroyResourcesAndQuit() {
        handler.post {
            if (_eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(_eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                if (_eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(_eglDisplay, _eglSurface)
                    _eglSurface = EGL14.EGL_NO_SURFACE
                }

                if (_eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(_eglDisplay, _eglContext)
                    _eglContext = EGL14.EGL_NO_CONTEXT
                }

                EGL14.eglTerminate(_eglDisplay)
                _eglDisplay = EGL14.EGL_NO_DISPLAY
            }
        }

        quitSafely()
        join()
    }

    private fun recreateSurface(width: Int, height: Int) {
        @Suppress("MemberVisibilityCanBePrivate")
        if (_eglDisplay == EGL14.EGL_NO_DISPLAY ||
                _eglContext == EGL14.EGL_NO_CONTEXT || _eglConfig == null) {
            return
        }

        if (_surfaceWidth == width && _surfaceHeight == height) {
            return
        }

        if (_eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(_eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(_eglDisplay, _eglSurface)
            _eglSurface = EGL14.EGL_NO_SURFACE
        }

        if (width > 0 && height > 0) {
            val attr = intArrayOf(EGL14.EGL_NONE)
            val surface = EGL14.eglCreateWindowSurface(
                    _eglDisplay, _eglConfig, _windowsSurface, attr, 0)
            if (surface == EGL14.EGL_NO_SURFACE) {
                throw UnsupportedOperationException("Unable to create egl surface")
            }

            if (!EGL14.eglMakeCurrent(_eglDisplay, surface, surface, _eglContext)) {
                val errorCode = EGL14.eglGetError()
                EGL14.eglDestroySurface(_eglDisplay, surface)
                throw UnsupportedOperationException("Failed to makeCurrent: $errorCode")
            }

            _eglSurface = surface
        }

        _surfaceWidth = width
        _surfaceHeight = height
    }
}