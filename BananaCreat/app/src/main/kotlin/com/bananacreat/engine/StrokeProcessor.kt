package com.bananacreat.engine

import android.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.bananacreat.model.ArtworkDocument
import com.bananacreat.model.BrushSettings
import com.bananacreat.model.BrushType
import kotlin.math.sqrt

class StrokeProcessor(
    private val document: ArtworkDocument,
    private val brushEngine: BrushEngine,
    private val undoStack: UndoStack
) {
    private var prevX = 0f
    private var prevY = 0f
    private var prevSmX = 0f
    private var prevSmY = 0f
    private var isFirstPoint = true
    private var activeCanvas: Canvas? = null

    fun beginStroke(x: Float, y: Float, pressure: Float, color: androidx.compose.ui.graphics.Color, brush: BrushSettings) {
        val layer = document.activeLayer() ?: return
        if (layer.isLocked) return
        undoStack.push(document)
        activeCanvas = Canvas(layer.bitmap)
        val argb = color.toArgb()
        prevX = x; prevY = y; prevSmX = x; prevSmY = y
        isFirstPoint = true
        brushEngine.beginStroke(activeCanvas!!, x, y, pressure, argb, brush)
        isFirstPoint = false
    }

    fun continueStroke(x: Float, y: Float, pressure: Float, color: androidx.compose.ui.graphics.Color, brush: BrushSettings) {
        val canvas = activeCanvas ?: return
        val argb = color.toArgb()

        val smooth = brush.smoothing
        val smX = prevSmX + (x - prevSmX) * (1f - smooth)
        val smY = prevSmY + (y - prevSmY) * (1f - smooth)

        brushEngine.continueStroke(canvas, prevSmX, prevSmY, smX, smY, pressure, argb, brush)
        prevSmX = smX; prevSmY = smY
        prevX = x; prevY = y
    }

    fun endStroke() {
        activeCanvas = null
    }

    fun fillLayer(color: androidx.compose.ui.graphics.Color) {
        val layer = document.activeLayer() ?: return
        if (layer.isLocked) return
        undoStack.push(document)
        val canvas = Canvas(layer.bitmap)
        canvas.drawColor(color.toArgb())
    }

    fun clearLayer() {
        val layer = document.activeLayer() ?: return
        if (layer.isLocked) return
        undoStack.push(document)
        val canvas = Canvas(layer.bitmap)
        canvas.drawColor(Color.Transparent.toArgb(), PorterDuff.Mode.CLEAR)
    }

    fun floodFill(x: Float, y: Float, color: androidx.compose.ui.graphics.Color, tolerance: Int = 30) {
        val layer = document.activeLayer() ?: return
        if (layer.isLocked) return
        undoStack.push(document)
        FloodFill.fill(layer.bitmap, x.toInt(), y.toInt(), color.toArgb(), tolerance)
    }
}

object FloodFill {
    fun fill(bitmap: Bitmap, startX: Int, startY: Int, fillColor: Int, tolerance: Int) {
        if (startX < 0 || startY < 0 || startX >= bitmap.width || startY >= bitmap.height) return
        val targetColor = bitmap.getPixel(startX, startY)
        if (colorsMatch(targetColor, fillColor, 0)) return

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val queue = ArrayDeque<Int>()
        val w = bitmap.width
        val h = bitmap.height

        fun idx(x: Int, y: Int) = y * w + x

        queue.add(idx(startX, startY))
        val visited = HashSet<Int>()
        visited.add(idx(startX, startY))

        while (queue.isNotEmpty()) {
            val pos = queue.removeFirst()
            val px = pos % w
            val py = pos / w
            pixels[pos] = fillColor

            val neighbors = listOf(
                px - 1 to py, px + 1 to py, px to py - 1, px to py + 1
            )
            for ((nx, ny) in neighbors) {
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                val ni = idx(nx, ny)
                if (ni in visited) continue
                if (colorsMatch(pixels[ni], targetColor, tolerance)) {
                    visited.add(ni)
                    queue.add(ni)
                }
            }
            if (queue.size > 500_000) break // safety cap
        }

        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun colorsMatch(c1: Int, c2: Int, tolerance: Int): Boolean {
        val dr = Math.abs(Color.red(c1) - Color.red(c2))
        val dg = Math.abs(Color.green(c1) - Color.green(c2))
        val db = Math.abs(Color.blue(c1) - Color.blue(c2))
        val da = Math.abs(Color.alpha(c1) - Color.alpha(c2))
        return dr + dg + db + da <= tolerance * 4
    }
}
