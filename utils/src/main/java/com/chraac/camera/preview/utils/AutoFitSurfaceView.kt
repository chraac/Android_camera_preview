/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chraac.camera.preview.utils

import android.content.Context
import android.graphics.ImageFormat.*
import android.util.AttributeSet
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.chraac.extsurfacetexture.ExtSurfaceTexture
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.async
import java.util.concurrent.atomic.AtomicInteger

/**
 * A [SurfaceView] that can be adjusted to a specified aspect ratio and
 * performs center-crop transformation of input frames.
 */
class AutoFitSurfaceView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : SurfaceView(context, attrs, defStyle),
        BufferQueueProducer,
        SurfaceTextureManager.FrameAvailableListener {

    override var surfaceCallback: SurfaceHolder.Callback?
        get() = outerSurfaceCallback
        set(value) {
            if (outerSurfaceCallback == null) {
                holder.addCallback(innerSurfaceCallback)
            }

            outerSurfaceCallback = value
        }

    // FIXME: set to screen default
    private var orientationInDegree = AtomicInteger(90)

    private var outerSurfaceCallback: SurfaceHolder.Callback? = null

    private var surfaceTextureManager: SurfaceTextureManager? = null

    private var glThread: GLHandlerThread? = null

    private val mainScope by lazy { MainScope() }

    private val innerSurfaceCallback = object : SurfaceHolder.Callback {

        @MainThread
        override fun surfaceCreated(holder: SurfaceHolder?) {
            var thread = glThread
            if (thread == null) {
                thread = GLHandlerThread("GLCameraFrameThread", getHolder().surface)
                glThread = thread
            }

            outerSurfaceCallback?.surfaceCreated(holder)
        }

        @MainThread
        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            glThread?.notifySurfaceSizeChange(width, height)
            outerSurfaceCallback?.surfaceChanged(holder, format, width, height)
        }

        @MainThread
        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            outerSurfaceCallback?.surfaceDestroyed(holder)
            glThread?.post {
                surfaceTextureManager?.close()
            }

            glThread?.close()
            glThread = null
        }
    }

    /**
     * Sets the size for this view. The size of the view will be
     * measured based on the ratio calculated from the parameters.
     * Attention: previous surface will be invalidate after method return
     *
     * @param width  Camera resolution horizontal size
     * @param height Camera resolution vertical size
     */
    @MainThread
    fun setPreviewSurfaceSizeAsync(width: Int, height: Int): Deferred<Surface> {
        require(width > 0 && height > 0) { "Size cannot be negative" }

        val glThread = glThread ?: throw IllegalStateException("glThread is null")
        return mainScope.async(glThread.handler.asCoroutineDispatcher()) {
            var manager = surfaceTextureManager
            if (manager == null || manager.size != Size(width, height)) {
                manager?.close()
                manager = SurfaceTextureManagerImpl(
                        ExtSurfaceTexture(width, height, YUV_420_888, 3, 0))
                manager.setFrameAvailableListener(
                        this@AutoFitSurfaceView, glThread.handler)
                surfaceTextureManager = manager
            }

            manager.surface
        }
    }

    @MainThread
    fun setPreviewOrientation(orientation: Int) = orientationInDegree.set(orientation)

    @WorkerThread
    override fun onFrameAvailable(surfaceTexture: SurfaceTextureManager) {
        val thread = glThread ?: return
        val surfaceTextureManager = this.surfaceTextureManager ?: return
        val drawer = thread.frameBegin()

        val orientation = orientationInDegree.get()
        if (orientation != 0) {
            drawer.save()
            val centerX = width / 2
            val centerY = height / 2
            drawer.translate(centerX.toFloat(), centerY.toFloat(), 0f)
            drawer.rotate(orientation.toFloat(), 0f, 0f, 1f)
            when (orientation) {
                0, 180 -> {
                    drawer.translate(-(centerX.toFloat()), -(centerY.toFloat()), 0f)
                }
                90, 270 -> {
                    drawer.translate(-(centerY.toFloat()), -(centerX.toFloat()), 0f)
                }
            }
        }

        drawer.draw(surfaceTextureManager)
        if (orientation != 0) {
            drawer.restore()
        }

        thread.frameEnd()
    }
}
