package com.chraac.advsurfacetexture

interface GLFunctions {

    fun glActiveTexture(texture: Int)

    fun glBindTexture(target: Int, texture: Int)

    fun glTexParameteri(target: Int, pname: Int, param: Int)

    fun glGetError(): Int

}