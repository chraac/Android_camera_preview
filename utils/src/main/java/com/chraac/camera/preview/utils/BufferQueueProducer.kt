package com.chraac.camera.preview.utils

import android.view.SurfaceHolder

interface BufferQueueProducer {
    var surfaceCallback: SurfaceHolder.Callback?
}