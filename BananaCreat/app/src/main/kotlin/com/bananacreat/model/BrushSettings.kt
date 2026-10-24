package com.bananacreat.model

data class BrushSettings(
    val name: String = "Round Brush",
    val type: BrushType = BrushType.ROUND,
    var size: Float = 20f,        // px
    var opacity: Float = 1f,      // 0..1
    var hardness: Float = 0.8f,   // 0..1 (edge softness)
    var spacing: Float = 0.1f,    // fraction of size
    var pressureSize: Boolean = true,
    var pressureOpacity: Boolean = false,
    var smoothing: Float = 0.5f,  // stroke smoothing 0..1
    var scatter: Float = 0f,      // random scatter
    var angle: Float = 0f,        // brush angle degrees
    var squish: Float = 1f,       // aspect ratio 0.1..1
    var jitter: Float = 0f        // color jitter
)

enum class BrushType {
    ROUND, FLAT, AIRBRUSH, PENCIL, WATERCOLOR, SMUDGE, ERASER, INK, CALLIGRAPHY, MARKER
}

val DefaultBrushes = listOf(
    BrushSettings("Round Brush", BrushType.ROUND, size = 20f, hardness = 0.9f),
    BrushSettings("Soft Brush", BrushType.ROUND, size = 40f, hardness = 0.2f, pressureOpacity = true),
    BrushSettings("Airbrush", BrushType.AIRBRUSH, size = 60f, hardness = 0f, spacing = 0.05f, pressureOpacity = true),
    BrushSettings("Pencil", BrushType.PENCIL, size = 8f, hardness = 0.95f, spacing = 0.08f),
    BrushSettings("Ink Pen", BrushType.INK, size = 10f, hardness = 1f, pressureSize = true),
    BrushSettings("Marker", BrushType.MARKER, size = 30f, hardness = 0.7f, opacity = 0.6f),
    BrushSettings("Flat Brush", BrushType.FLAT, size = 25f, hardness = 0.85f, squish = 0.3f, angle = 45f),
    BrushSettings("Calligraphy", BrushType.CALLIGRAPHY, size = 20f, hardness = 0.9f, squish = 0.2f, angle = 55f),
    BrushSettings("Watercolor", BrushType.WATERCOLOR, size = 50f, hardness = 0.1f, opacity = 0.4f, pressureOpacity = true),
    BrushSettings("Eraser", BrushType.ERASER, size = 30f, hardness = 0.8f)
)
