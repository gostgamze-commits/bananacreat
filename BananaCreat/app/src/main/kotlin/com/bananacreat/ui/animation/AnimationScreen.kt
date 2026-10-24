package com.bananacreat.ui.animation

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bananacreat.AnimationViewModel
import kotlinx.coroutines.launch

@Composable
fun AnimationScreen(
    viewModel: AnimationViewModel,
    navController: NavHostController
) {
    val project by viewModel.animationProject.collectAsState()
    val currentFrameIndex by viewModel.currentFrameIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackTime by viewModel.playbackTime.collectAsState()
    val brushColor by viewModel.brushColor.collectAsState()
    val currentBrush by viewModel.currentBrush.collectAsState()
    val canvasVersion by viewModel.canvasVersion.collectAsState()
    val onionSkinOpacity by viewModel.onionSkinOpacity.collectAsState()
    val showOnionSkin by viewModel.showOnionSkin.collectAsState()

    var showBrushPanel by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    var zoom by remember { mutableStateOf(1f) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(project?.name ?: "Animation", maxLines = 1)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.saveAnimation()
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.playAnimation() }, enabled = !isPlaying) {
                        Icon(Icons.Filled.PlayArrow, "Play")
                    }
                    IconButton(onClick = { viewModel.pauseAnimation() }, enabled = isPlaying) {
                        Icon(Icons.Filled.Pause, "Pause")
                    }
                    IconButton(onClick = { viewModel.stopAnimation() }) {
                        Icon(Icons.Filled.Stop, "Stop")
                    }
                    IconButton(onClick = { showBrushPanel = !showBrushPanel }) {
                        Icon(Icons.Filled.Brush, "Brush")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2A2A2A)
                )
            )
        },
        bottomBar = {
            if (project != null) {
                TimelineEditor(
                    project = project!!,
                    currentFrameIndex = currentFrameIndex,
                    onFrameSelect = { viewModel.selectFrame(it) },
                    onAddFrame = { viewModel.addFrame() },
                    onDuplicateFrame = { viewModel.addFrame(duplicateFrom = currentFrameIndex) },
                    onDeleteFrame = { viewModel.deleteFrame(currentFrameIndex) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (project != null) {
                val currentFrame = project!!.frames.getOrNull(currentFrameIndex)
                if (currentFrame != null) {
                    DrawingCanvas(
                        bitmap = currentFrame.bitmap,
                        onionSkinBitmaps = if (showOnionSkin) {
                            listOfNotNull(
                                project!!.frames.getOrNull(currentFrameIndex - 1)?.bitmap,
                                project!!.frames.getOrNull(currentFrameIndex + 1)?.bitmap
                            )
                        } else emptyList(),
                        onionSkinOpacity = onionSkinOpacity,
                        zoom = zoom,
                        panX = panX,
                        panY = panY,
                        onDrawing = { x, y, pressure ->
                            val canvasX = (x - panX) / zoom
                            val canvasY = (y - panY) / zoom
                            viewModel.continueStroke(canvasX, canvasY, pressure)
                        },
                        onStrokeStart = { x, y ->
                            val canvasX = (x - panX) / zoom
                            val canvasY = (y - panY) / zoom
                            viewModel.beginStroke(canvasX, canvasY, 0.8f)
                        },
                        onStrokeEnd = { viewModel.endStroke() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                if (showColorPicker) {
                    Surface(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color(0xFF2A2A2A))
                            .clickable { showColorPicker = false }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(brushColor)
                        )
                    }
                }
                if (showBrushPanel) {
                    Surface(
                        modifier = Modifier.width(200.dp),
                        color = Color(0xFF2A2A2A)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("FPS: ${project?.fps ?: 24}")
                            Slider(
                                value = (project?.fps ?: 24).toFloat(),
                                onValueChange = { viewModel.setFPS(it.toInt()) },
                                valueRange = 12f..60f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                Checkbox(
                                    checked = showOnionSkin,
                                    onCheckedChange = { viewModel.toggleOnionSkin() }
                                )
                                Text("Onion Skin")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawingCanvas(
    bitmap: Bitmap,
    onionSkinBitmaps: List<Bitmap>,
    onionSkinOpacity: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    onDrawing: (Float, Float, Float) -> Unit,
    onStrokeStart: (Float, Float) -> Unit,
    onStrokeEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDrawing by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDrawing = true
                        onStrokeStart(offset.x, offset.y)
                    },
                    onDrag = { change, _ ->
                        onDrawing(change.position.x, change.position.y, 0.8f)
                    },
                    onDragEnd = {
                        isDrawing = false
                        onStrokeEnd()
                    }
                )
            }
    ) {
        val mainImage = bitmap.asImageBitmap()

        onionSkinBitmaps.forEach { prevBmp ->
            drawImage(
                image = prevBmp.asImageBitmap(),
                topLeftOffset = androidx.compose.ui.geometry.Offset(panX, panY),
                alpha = onionSkinOpacity
            )
        }

        drawImage(
            image = mainImage,
            topLeftOffset = androidx.compose.ui.geometry.Offset(panX, panY),
            alpha = 1f
        )
    }
}

@Composable
fun TimelineEditor(
    project: com.bananacreat.model.AnimationProject,
    currentFrameIndex: Int,
    onFrameSelect: (Int) -> Unit,
    onAddFrame: () -> Unit,
    onDuplicateFrame: () -> Unit,
    onDeleteFrame: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            project.frames.forEachIndexed { index, frame ->
                FrameThumbnail(
                    index = index,
                    isSelected = index == currentFrameIndex,
                    onClick = { onFrameSelect(index) }
                )
            }

            IconButton(onClick = onAddFrame, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Add, "Add Frame", tint = Color.White)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(onClick = onDuplicateFrame, modifier = Modifier.weight(1f)) {
                Text("Duplicate")
            }
            Button(onClick = onDeleteFrame, modifier = Modifier.weight(1f)) {
                Text("Delete")
            }
        }
    }
}

@Composable
fun FrameThumbnail(
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                if (isSelected) Color(0xFFFFB900) else Color(0xFF3A3A3A)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            (index + 1).toString(),
            color = if (isSelected) Color.Black else Color.White
        )
    }
}
