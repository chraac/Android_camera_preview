package com.example.android.camera.utils

import android.view.Surface
import android.view.SurfaceHolder

interface BufferQueueProducer {
    val producerSurface: Surface
    var surfaceCallback: SurfaceHolder.Callback?
}