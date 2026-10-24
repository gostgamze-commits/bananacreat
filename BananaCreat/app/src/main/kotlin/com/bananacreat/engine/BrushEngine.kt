package com.bananacreat.engine

import android.graphics.*
import com.bananacreat.model.BrushSettings
import com.bananacreat.model.BrushType
import kotlin.math.*

class BrushEngine {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val brushBitmap = HashMap<String, Bitmap>()

    fun beginStroke(canvas: Canvas, x: Float, y: Float, pressure: Float, color: Int, brush: BrushSettings) {
        drawDab(canvas, x, y, pressure, color, brush)
    }

    fun continueStroke(
        canvas: Canvas,
        prevX: Float, prevY: Float,
        x: Float, y: Float,
        pressure: Float,
        color: Int,
        brush: BrushSettings
    ) {
        val dx = x - prevX
        val dy = y - prevY
        val dist = sqrt(dx * dx + dy * dy)
        val step = brush.size * brush.spacing.coerceAtLeast(0.02f)

        if (dist < step) {
            drawDab(canvas, x, y, pressure, color, brush)
            return
        }

        val steps = (dist / step).toInt().coerceAtLeast(1)
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val ix = prevX + dx * t
            val iy = prevY + dy * t
            drawDab(canvas, ix, iy, pressure, color, brush)
        }
    }

    private fun drawDab(canvas: Canvas, x: Float, y: Float, pressure: Float, color: Int, brush: BrushSettings) {
        val size = if (brush.pressureSize) brush.size * pressure.coerceIn(0.05f, 1f) else brush.size
        val opacity = if (brush.pressureOpacity) brush.opacity * pressure.coerceIn(0.05f, 1f) else brush.opacity
        val radius = (size / 2f).coerceAtLeast(0.5f)

        val sx = if (brush.scatter > 0) x + (Math.random() - 0.5f).toFloat() * brush.scatter * size else x
        val sy = if (brush.scatter > 0) y + (Math.random() - 0.5f).toFloat() * brush.scatter * size else y

        val bmp = getBrushBitmap(brush, radius, color)

        paint.alpha = (opacity * 255).toInt()

        if (brush.type == BrushType.ERASER) {
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            paint.alpha = (opacity * 255).toInt()
        } else {
            paint.xfermode = null
        }

        val matrix = Matrix()
        if (brush.angle != 0f || brush.squish != 1f) {
            matrix.postScale(1f, brush.squish, radius, radius)
            matrix.postRotate(brush.angle, radius, radius)
            matrix.postTranslate(sx - radius, sy - radius)
            canvas.drawBitmap(bmp, matrix, paint)
        } else {
            canvas.drawBitmap(bmp, sx - radius, sy - radius, paint)
        }

        paint.xfermode = null
    }

    private fun getBrushBitmap(brush: BrushSettings, radius: Float, color: Int): Bitmap {
        val key = "${brush.type}_${radius.toInt()}_${brush.hardness}"
        brushBitmap[key]?.let { if (!it.isRecycled) return it }

        val size = (radius * 2).toInt().coerceAtLeast(2)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        when (brush.type) {
            BrushType.AIRBRUSH -> {
                val shader = RadialGradient(
                    radius, radius, radius,
                    intArrayOf(color, Color.argb(0, Color.red(color), Color.green(color), Color.blue(color))),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
                p.shader = shader
                c.drawCircle(radius, radius, radius, p)
            }
            BrushType.PENCIL -> {
                p.color = color
                p.alpha = 180
                val grain = Paint(Paint.ANTI_ALIAS_FLAG)
                grain.color = color
                c.drawCircle(radius, radius, radius * brush.hardness, p)
                // pencil texture: random dots
                val rng = java.util.Random(42)
                grain.alpha = 100
                repeat((radius * radius * 0.3).toInt()) {
                    val tx = rng.nextFloat() * size
                    val ty = rng.nextFloat() * size
                    val d = sqrt((tx - radius) * (tx - radius) + (ty - radius) * (ty - radius))
                    if (d < radius) c.drawCircle(tx, ty, 0.5f, grain)
                }
            }
            BrushType.WATERCOLOR -> {
                val hardEdge = radius * brush.hardness
                val shader = RadialGradient(
                    radius, radius, radius,
                    intArrayOf(
                        Color.argb(200, Color.red(color), Color.green(color), Color.blue(color)),
                        Color.argb(60, Color.red(color), Color.green(color), Color.blue(color)),
                        Color.argb(0, Color.red(color), Color.green(color), Color.blue(color))
                    ),
                    floatArrayOf(0f, 0.6f, 1f),
                    Shader.TileMode.CLAMP
                )
                p.shader = shader
                c.drawCircle(radius, radius, radius, p)
            }
            BrushType.INK -> {
                p.color = color
                val hardEdge = radius * brush.hardness.coerceAtLeast(0.95f)
                c.drawCircle(radius, radius, hardEdge, p)
            }
            BrushType.MARKER -> {
                p.color = color
                p.alpha = 200
                val softRadius = radius * 0.9f
                c.drawCircle(radius, radius, softRadius, p)
                p.alpha = 80
                c.drawCircle(radius, radius, radius, p)
            }
            else -> {
                // ROUND, FLAT, CALLIGRAPHY, SMUDGE, ERASER
                val hardEdge = radius * brush.hardness
                val softEdge = radius - hardEdge
                if (softEdge > 0.5f) {
                    val shader = RadialGradient(
                        radius, radius, radius,
                        intArrayOf(color, color, Color.argb(0, Color.red(color), Color.green(color), Color.blue(color))),
                        floatArrayOf(0f, brush.hardness, 1f),
                        Shader.TileMode.CLAMP
                    )
                    p.shader = shader
                } else {
                    p.color = color
                }
                c.drawCircle(radius, radius, radius, p)
            }
        }

        brushBitmap[key] = bmp
        return bmp
    }

    fun clearCache() {
        brushBitmap.values.forEach { if (!it.isRecycled) it.recycle() }
        brushBitmap.clear()
    }
}
