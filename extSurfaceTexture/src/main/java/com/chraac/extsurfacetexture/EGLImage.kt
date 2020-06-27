package com.chraac.extsurfacetexture

import android.opengl.EGLObjectHandle

@Suppress("EqualsOrHashCode")
class EGLImage private constructor(
        handler: Long
) : EGLObjectHandle(handler) {

    override fun equals(other: Any?): Boolean {
        if (other !is EGLImage) {
            return super.equals(other)
        }

        return other.nativeHandle == nativeHandle
    }

}