package com.chraac.advsurfacetexture

import android.annotation.TargetApi
import android.opengl.EGLObjectHandle

@Suppress("EqualsOrHashCode")
@TargetApi(21)
class EGLImageKHR private constructor(
        handler: Long
) : EGLObjectHandle(handler) {

    override fun equals(other: Any?): Boolean {
        if (other !is EGLImageKHR) {
            return super.equals(other)
        }

        return other.nativeHandle == nativeHandle
    }

}