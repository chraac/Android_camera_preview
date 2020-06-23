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

package com.example.android.camera.utils

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

private val TAG = AutoFitSurfaceView::class.java.simpleName

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
        SurfaceTexture.OnFrameAvailableListener {

    override val producerSurface: Surface
        get() = surface

    override var surfaceCallback: SurfaceHolder.Callback?
        get() = outerSurfaceCallback
        set(value) {
            if (outerSurfaceCallback == null) {
                holder.addCallback(innerSurfaceCallback)
            }

            outerSurfaceCallback = value
        }


    private var aspectRatio = 0f


    private var outerSurfaceCallback: SurfaceHolder.Callback? = null

    private val surfaceTextureWrapper: SurfaceTextureExt = SurfaceTextureWrapper()

    private val surface = Surface(surfaceTextureWrapper.surfaceTexture)
    private var glThread: GLHandlerThread? = null

    private val mainScope = MainScope()

    private val innerSurfaceCallback = object : SurfaceHolder.Callback {

        @MainThread
        override fun surfaceCreated(holder: SurfaceHolder?) {
            var thread = glThread
            if (thread == null) {
                thread = GLHandlerThread("GLCameraFrameThread", getHolder().surface)
                surfaceTextureWrapper.surfaceTexture.setOnFrameAvailableListener(
                        this@AutoFitSurfaceView, thread.handler)
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
            glThread?.run {
                surfaceTextureWrapper.deleteGLTexture()
            }

            glThread?.close()
            glThread = null
        }
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be
     * measured based on the ratio calculated from the parameters.
     *
     * @param width  Camera resolution horizontal size
     * @param height Camera resolution vertical size
     */
    fun setAspectRatio(width: Int, height: Int) {
        require(width > 0 && height > 0) { "Size cannot be negative" }
        aspectRatio = width.toFloat() / height.toFloat()
        holder.setFixedSize(width, height)
        glThread?.handler?.post {
            surfaceTextureWrapper.size = Size(width, height)
        }
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (aspectRatio == 0f) {
            setMeasuredDimension(width, height)
        } else {

            // Performs center-crop transformation of the camera frames
            val newWidth: Int
            val newHeight: Int
            val actualRatio = if (width > height) aspectRatio else 1f / aspectRatio
            if (width < height * actualRatio) {
                newHeight = height
                newWidth = (height * actualRatio).roundToInt()
            } else {
                newWidth = width
                newHeight = (width / actualRatio).roundToInt()
            }

            Log.d(TAG, "Measured dimensions set: $newWidth x $newHeight")
            setMeasuredDimension(newWidth, newHeight)
        }
    }

    @WorkerThread
    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        val thread = glThread ?: return
        thread.drawer.draw(surfaceTextureWrapper)
        thread.swapBuffer()
    }
}
