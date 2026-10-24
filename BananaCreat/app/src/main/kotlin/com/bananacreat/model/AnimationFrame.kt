package com.bananacreat.model

import android.graphics.Bitmap
import java.util.UUID

data class AnimationFrame(
    val id: String = UUID.randomUUID().toString(),
    val bitmap: Bitmap,
    val duration: Int = 100, // milliseconds
    val index: Int = 0
)
