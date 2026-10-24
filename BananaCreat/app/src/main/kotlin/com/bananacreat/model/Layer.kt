package com.bananacreat.model

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import java.util.UUID

data class Layer(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "Layer",
    var bitmap: Bitmap,
    var isVisible: Boolean = true,
    var opacity: Float = 1f,
    var blendMode: LayerBlendMode = LayerBlendMode.NORMAL,
    var isLocked: Boolean = false,
    var isMask: Boolean = false
) {
    fun composite(canvas: Canvas, paint: Paint) {
        if (!isVisible) return
        paint.alpha = (opacity * 255).toInt()
        paint.xfermode = blendMode.toXfermode()
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        paint.xfermode = null
        paint.alpha = 255
    }
}

enum class LayerBlendMode(val label: String) {
    NORMAL("Normal"),
    MULTIPLY("Multiply"),
    SCREEN("Screen"),
    OVERLAY("Overlay"),
    DARKEN("Darken"),
    LIGHTEN("Lighten"),
    COLOR_DODGE("Color Dodge"),
    COLOR_BURN("Color Burn"),
    HARD_LIGHT("Hard Light"),
    SOFT_LIGHT("Soft Light"),
    DIFFERENCE("Difference"),
    EXCLUSION("Exclusion"),
    HUE("Hue"),
    SATURATION("Saturation"),
    COLOR("Color"),
    LUMINOSITY("Luminosity"),
    ADD("Add"),
    SUBTRACT("Subtract");

    fun toXfermode(): PorterDuffXfermode? = when (this) {
        NORMAL -> null
        MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        OVERLAY -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
        DARKEN -> PorterDuffXfermode(PorterDuff.Mode.DARKEN)
        LIGHTEN -> PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
        COLOR_DODGE -> PorterDuffXfermode(PorterDuff.Mode.SCREEN) // closest available
        COLOR_BURN -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        HARD_LIGHT -> PorterDuffXfermode(PorterDuff.Mode.HARD_LIGHT)
        SOFT_LIGHT -> PorterDuffXfermode(PorterDuff.Mode.SOFT_LIGHT)
        DIFFERENCE -> PorterDuffXfermode(PorterDuff.Mode.DIFFERENCE)
        EXCLUSION -> PorterDuffXfermode(PorterDuff.Mode.EXCLUSION)
        HUE -> null
        SATURATION -> PorterDuffXfermode(PorterDuff.Mode.SATURATION)
        COLOR -> PorterDuffXfermode(PorterDuff.Mode.COLOR)
        LUMINOSITY -> PorterDuffXfermode(PorterDuff.Mode.LUMINOSITY)
        ADD -> PorterDuffXfermode(PorterDuff.Mode.ADD)
        SUBTRACT -> null
    }
}
