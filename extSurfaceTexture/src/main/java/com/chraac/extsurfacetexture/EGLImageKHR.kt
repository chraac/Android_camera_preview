package com.chraac.extsurfacetexture

import android.opengl.EGLObjectHandle

@Suppress("EqualsOrHashCode")
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