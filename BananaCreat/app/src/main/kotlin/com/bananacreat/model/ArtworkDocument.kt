package com.bananacreat.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.util.UUID

data class ArtworkDocument(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "Untitled",
    val width: Int = 2048,
    val height: Int = 2048,
    val layers: MutableList<Layer> = mutableListOf(),
    var activeLayerIndex: Int = 0,
    var createdAt: Long = System.currentTimeMillis(),
    var modifiedAt: Long = System.currentTimeMillis()
) {
    fun activeLayer(): Layer? = layers.getOrNull(activeLayerIndex)

    fun composite(): Bitmap {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (layer in layers) {
            layer.composite(canvas, paint)
        }
        return result
    }

    fun addLayer(name: String = "Layer ${layers.size + 1}"): Layer {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val layer = Layer(name = name, bitmap = bmp)
        layers.add(activeLayerIndex + 1, layer)
        activeLayerIndex = activeLayerIndex + 1
        modifiedAt = System.currentTimeMillis()
        return layer
    }

    fun deleteLayer(index: Int) {
        if (layers.size <= 1) return
        layers[index].bitmap.recycle()
        layers.removeAt(index)
        activeLayerIndex = (activeLayerIndex).coerceIn(0, layers.size - 1)
        modifiedAt = System.currentTimeMillis()
    }

    fun moveLayer(from: Int, to: Int) {
        if (from == to) return
        val layer = layers.removeAt(from)
        layers.add(to, layer)
        activeLayerIndex = to
        modifiedAt = System.currentTimeMillis()
    }

    fun mergeDown(index: Int) {
        if (index <= 0 || index >= layers.size) return
        val top = layers[index]
        val bottom = layers[index - 1]
        val canvas = Canvas(bottom.bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.alpha = (top.opacity * 255).toInt()
        paint.xfermode = top.blendMode.toXfermode()
        canvas.drawBitmap(top.bitmap, 0f, 0f, paint)
        top.bitmap.recycle()
        layers.removeAt(index)
        activeLayerIndex = index - 1
        modifiedAt = System.currentTimeMillis()
    }
}
