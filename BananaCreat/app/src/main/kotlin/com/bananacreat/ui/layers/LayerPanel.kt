package com.bananacreat.ui.layers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bananacreat.model.ArtworkDocument
import com.bananacreat.model.LayerBlendMode

@Composable
fun LayerPanel(
    document: ArtworkDocument,
    onLayerSelect: (Int) -> Unit,
    onLayerDelete: (Int) -> Unit,
    onLayerAdd: () -> Unit,
    onLayerOpacityChanged: (Int, Float) -> Unit,
    onLayerVisibilityChanged: (Int, Boolean) -> Unit,
    onLayerBlendModeChanged: (Int, LayerBlendMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedLayerIndex by remember { mutableStateOf(-1) }

    Surface(
        modifier = modifier
            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
        color = Color(0xFF2A2A2A),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Layers",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onLayerAdd,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Filled.Add, "Add Layer", tint = Color.White)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .padding(horizontal = 8.dp),
                reverseLayout = true
            ) {
                itemsIndexed(document.layers) { index, layer ->
                    LayerItem(
                        layer = layer,
                        isActive = index == document.activeLayerIndex,
                        isExpanded = expandedLayerIndex == index,
                        onClick = {
                            onLayerSelect(index)
                            expandedLayerIndex = -1
                        },
                        onExpandClick = {
                            expandedLayerIndex = if (expandedLayerIndex == index) -1 else index
                        },
                        onDelete = { onLayerDelete(index) },
                        onVisibilityToggle = { onLayerVisibilityChanged(index, it) },
                        onOpacityChanged = { onLayerOpacityChanged(index, it) },
                        onBlendModeChanged = { onLayerBlendModeChanged(index, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun LayerItem(
    layer: com.bananacreat.model.Layer,
    isActive: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onExpandClick: () -> Unit,
    onDelete: () -> Unit,
    onVisibilityToggle: (Boolean) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onBlendModeChanged: (LayerBlendMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isActive) Color(0xFF3A3A3A) else Color(0xFF2A2A2A),
                    RoundedCornerShape(4.dp)
                )
                .clickable(onClick = onClick)
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onVisibilityToggle(!layer.isVisible) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (layer.isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        "Visibility",
                        tint = if (layer.isVisible) Color.White else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    layer.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(
                    onClick = onExpandClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        "Expand",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(4.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Opacity", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Text("${(layer.opacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Slider(
                    value = layer.opacity,
                    onValueChange = onOpacityChanged,
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Blend Mode", style = MaterialTheme.typography.labelSmall, color = Color.White)
                BlendModeSelector(
                    currentMode = layer.blendMode,
                    onModeSelected = onBlendModeChanged
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000))
                    ) {
                        Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BlendModeSelector(
    currentMode: LayerBlendMode,
    onModeSelected: (LayerBlendMode) -> Unit
) {
    val modes = LayerBlendMode.values()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp)
            .background(Color(0xFF3A3A3A), RoundedCornerShape(4.dp))
            .verticalScroll(rememberScrollState())
    ) {
        modes.forEach { mode ->
            Text(
                mode.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (mode == currentMode) Color(0xFFFFB900) else Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onModeSelected(mode) }
                    .padding(8.dp)
            )
        }
    }
}
