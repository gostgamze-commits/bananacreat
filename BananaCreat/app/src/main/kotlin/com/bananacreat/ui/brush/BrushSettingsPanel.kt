package com.bananacreat.ui.brush

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bananacreat.model.BrushSettings
import com.bananacreat.model.DefaultBrushes

@Composable
fun BrushSettingsPanel(
    brush: BrushSettings,
    onSizeChanged: (Float) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onBrushSelected: (BrushSettings) -> Unit,
    modifier: Modifier = Modifier
) {
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
                "Brushes",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DefaultBrushes.forEach { brushOption ->
                    BrushOption(
                        name = brushOption.name,
                        isSelected = brush.name == brushOption.name,
                        onClick = { onBrushSelected(brushOption) }
                    )
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.3f))

            Text(
                "Settings",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )

            SliderWithLabel(
                label = "Size",
                value = brush.size,
                onValueChange = onSizeChanged,
                valueRange = 1f..200f
            )

            SliderWithLabel(
                label = "Opacity",
                value = brush.opacity,
                onValueChange = onOpacityChanged,
                valueRange = 0f..1f
            )

            SliderWithLabel(
                label = "Hardness",
                value = brush.hardness,
                onValueChange = { /* Read-only for now */ },
                valueRange = 0f..1f,
                enabled = false
            )

            SliderWithLabel(
                label = "Smoothing",
                value = brush.smoothing,
                onValueChange = { /* Read-only for now */ },
                valueRange = 0f..1f,
                enabled = false
            )
        }
    }
}

@Composable
fun BrushOption(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) Color(0xFFFFB900) else Color(0xFF3A3A3A),
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color.Black else Color.White
        )
    }
}

@Composable
fun SliderWithLabel(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true
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
                String.format("%.1f", value),
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
                .padding(top = 4.dp),
            enabled = enabled
        )
    }
}
