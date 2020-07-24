package com.chraac.advsurfacetexture

import androidx.annotation.RestrictTo

internal interface NativeObject {

    @set:RestrictTo(RestrictTo.Scope.SUBCLASSES)
    @get:RestrictTo(RestrictTo.Scope.SUBCLASSES)
    var native: Long

}