package com.chraac.camera.preview.utils

import android.opengl.GLES20
import com.chraac.advsurfacetexture.GLFunctions

class GLES20Functions : GLFunctions {

    override fun glActiveTexture(texture: Int) = GLES20.glActiveTexture(texture)

    override fun glBindTexture(target: Int, texture: Int) = GLES20.glBindTexture(target, texture)

    override fun glTexParameteri(target: Int, pname: Int, param: Int) =
            GLES20.glTexParameteri(target, pname, param)

    override fun glGetError(): Int = GLES20.glGetError()

}