package com.bananacreat.ui.color

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ColorPickerPanel(
    color: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var brightness by remember { mutableStateOf(1f) }
    var hexInput by remember { mutableStateOf(color.toHexString()) }

    LaunchedEffect(color) {
        val rgb = color.toArgb()
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        brightness = max / 255f
        saturation = if (max == 0) 0f else (max - min).toFloat() / max
        hue = when {
            max == min -> 0f
            max == r -> ((g - b).toFloat() / (max - min) * 60) % 360
            max == g -> ((b - r).toFloat() / (max - min) * 60 + 120)
            else -> ((r - g).toFloat() / (max - min) * 60 + 240)
        }
    }

    Surface(
        modifier = modifier
            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
            .verticalScroll(rememberScrollState()),
        color = Color(0xFF2A2A2A),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Color",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(color, RoundedCornerShape(8.dp))
            )

            HSVSlider(label = "Hue", value = hue, onValueChange = { h ->
                hue = h
                onColorChanged(hsvToColor(h, saturation, brightness))
            }, valueRange = 0f..360f)

            HSVSlider(label = "Saturation", value = saturation, onValueChange = { s ->
                saturation = s
                onColorChanged(hsvToColor(hue, s, brightness))
            }, valueRange = 0f..1f)

            HSVSlider(label = "Brightness", value = brightness, onValueChange = { b ->
                brightness = b
                onColorChanged(hsvToColor(hue, saturation, b))
            }, valueRange = 0f..1f)

            TextField(
                value = hexInput,
                onValueChange = { hex ->
                    hexInput = hex
                    if (hex.length == 6) {
                        try {
                            val color = Color(android.graphics.Color.parseColor("#$hex"))
                            onColorChanged(color)
                        } catch (e: Exception) {
                            // Invalid hex
                        }
                    }
                },
                label = { Text("Hex") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(),
                prefix = { Text("#") },
                singleLine = true
            )
        }
    }
}

@Composable
fun HSVSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Text(
                String.format("%.0f", value),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }
}

fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1f - ((h / 60f) % 2f - 1f).coerceIn(-1f, 1f).let { if (it < 0) -it else it })
    val m = v - c

    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = ((r + m) * 255).roundToInt() / 255f,
        green = ((g + m) * 255).roundToInt() / 255f,
        blue = ((b + m) * 255).roundToInt() / 255f
    )
}

fun Color.toHexString(): String {
    val argb = this.toArgb()
    return String.format("%06X", (argb and 0xFFFFFF))
}
