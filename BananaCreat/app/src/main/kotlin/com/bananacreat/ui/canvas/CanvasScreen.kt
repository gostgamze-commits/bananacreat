package com.bananacreat.ui.canvas

import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import com.bananacreat.CanvasViewModel
import com.bananacreat.model.BrushSettings
import com.bananacreat.ui.brush.BrushSettingsPanel
import com.bananacreat.ui.color.ColorPickerPanel
import com.bananacreat.ui.layers.LayerPanel
import kotlinx.coroutines.launch

@Composable
fun CanvasScreen(
    viewModel: CanvasViewModel,
    navController: NavHostController
) {
    val document by viewModel.document.collectAsState()
    val canvasVersion by viewModel.canvasVersion.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val brushColor by viewModel.brushColor.collectAsState()
    val currentBrush by viewModel.currentBrush.collectAsState()

    var showBrushPanel by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLayerPanel by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    var zoom by remember { mutableStateOf(1f) }

    var compositeCache by remember { mutableStateOf<Bitmap?>(null) }
    var cacheVersion by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(canvasVersion) {
        compositeCache?.recycle()
        document?.composite()?.let {
            compositeCache = it
            cacheVersion++
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Project Saved") },
            text = { Text("${document?.name} saved successfully.") },
            confirmButton = {
                Button(onClick = { showSaveDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showExportDialog && document != null) {
        ExportDialog(
            onExportPng = { uri ->
                viewModel.exportPng(uri)
                showExportDialog = false
            },
            onExportJpeg = { uri ->
                viewModel.exportJpeg(uri)
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        document?.name ?: "Canvas",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.saveDocument()
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = canUndo
                    ) {
                        Icon(Icons.Filled.Undo, "Undo")
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = canRedo
                    ) {
                        Icon(Icons.Filled.Redo, "Redo")
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
            BottomAppBar(
                containerColor = Color(0xFF2A2A2A),
                contentPadding = PaddingValues(8.dp)
            ) {
                IconButton(onClick = { showColorPicker = !showColorPicker }) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(brushColor, shape = MaterialTheme.shapes.small)
                    )
                }
                IconButton(onClick = { showLayerPanel = !showLayerPanel }) {
                    Icon(Icons.Filled.Layers, "Layers")
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    scope.launch {
                        viewModel.saveDocument()
                        showSaveDialog = true
                    }
                }) {
                    Icon(Icons.Filled.Save, "Save")
                }
                IconButton(onClick = { showExportDialog = true }) {
                    Icon(Icons.Filled.FileDownload, "Export")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main canvas area
            DrawingCanvas(
                document = document,
                compositeCache = compositeCache,
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
                onPan = { dx, dy ->
                    panX += dx
                    panY += dy
                },
                onZoom = { zoomFactor ->
                    zoom = (zoom * zoomFactor).coerceIn(0.5f, 5f)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )

            // Right panels
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(padding)
                    .padding(8.dp)
            ) {
                if (showBrushPanel) {
                    BrushSettingsPanel(
                        brush = currentBrush,
                        onSizeChanged = { viewModel.setBrushSize(it) },
                        onOpacityChanged = { viewModel.setBrushOpacity(it) },
                        onBrushSelected = { viewModel.setBrush(it) },
                        modifier = Modifier.width(200.dp)
                    )
                }

                if (showColorPicker) {
                    ColorPickerPanel(
                        color = brushColor,
                        onColorChanged = { viewModel.setBrushColor(it) },
                        modifier = Modifier.width(200.dp)
                    )
                }

                if (showLayerPanel && document != null) {
                    LayerPanel(
                        document = document!!,
                        onLayerSelect = { viewModel.setActiveLayer(it) },
                        onLayerDelete = { viewModel.deleteLayer(it) },
                        onLayerAdd = { viewModel.addLayer() },
                        onLayerOpacityChanged = { idx, opacity ->
                            viewModel.setLayerOpacity(idx, opacity)
                        },
                        onLayerVisibilityChanged = { idx, visible ->
                            viewModel.setLayerVisible(idx, visible)
                        },
                        onLayerBlendModeChanged = { idx, mode ->
                            viewModel.setLayerBlendMode(idx, mode)
                        },
                        modifier = Modifier.width(220.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DrawingCanvas(
    document: com.bananacreat.model.ArtworkDocument?,
    compositeCache: Bitmap?,
    zoom: Float,
    panX: Float,
    panY: Float,
    onDrawing: (Float, Float, Float) -> Unit,
    onStrokeStart: (Float, Float) -> Unit,
    onStrokeEnd: () -> Unit,
    onPan: (Float, Float) -> Unit,
    onZoom: (Float) -> Unit,
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
        if (compositeCache != null) {
            val imageBitmap = compositeCache.asImageBitmap()
            drawImage(
                image = imageBitmap,
                topLeftOffset = androidx.compose.ui.geometry.Offset(panX, panY),
                alpha = 1f
            )
        }
    }
}

@Composable
fun ExportDialog(
    onExportPng: (android.net.Uri) -> Unit,
    onExportJpeg: (android.net.Uri) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export") },
        text = {
            Text("Export as PNG or JPEG. File will be saved to Downloads.")
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val timestamp = System.currentTimeMillis()
                        val uri = android.net.Uri.parse("file:///sdcard/Download/bananacreat_$timestamp.png")
                        onExportPng(uri)
                        onDismiss()
                    }
                ) {
                    Text("PNG")
                }
                Button(
                    onClick = {
                        val timestamp = System.currentTimeMillis()
                        val uri = android.net.Uri.parse("file:///sdcard/Download/bananacreat_$timestamp.jpg")
                        onExportJpeg(uri)
                        onDismiss()
                    }
                ) {
                    Text("JPEG")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
