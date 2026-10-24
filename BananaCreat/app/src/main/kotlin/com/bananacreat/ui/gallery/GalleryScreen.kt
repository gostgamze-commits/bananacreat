package com.bananacreat.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.bananacreat.CanvasViewModel
import com.bananacreat.data.GalleryItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GalleryScreen(
    viewModel: CanvasViewModel,
    navController: NavHostController
) {
    val galleryItems by viewModel.galleryItems.collectAsState()
    val showNewDocDialog = remember { mutableStateOf(false) }
    val showDeleteConfirm = remember { mutableStateOf<GalleryItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadGallery()
    }

    if (showNewDocDialog.value) {
        NewDocumentDialog(
            onDismiss = { showNewDocDialog.value = false },
            onCreate = { name, width, height ->
                viewModel.newDocument(name, width, height)
                showNewDocDialog.value = false
                navController.navigate("canvas")
            }
        )
    }

    showDeleteConfirm.value?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm.value = null },
            title = { Text("Delete Project?") },
            text = { Text("${item.name} will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDocument(item.id)
                        showDeleteConfirm.value = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm.value = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BananaCreat", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2A2A2A)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewDocDialog.value = true },
                containerColor = Color(0xFFFFB900),
                contentColor = Color.Black
            ) {
                Icon(Icons.Filled.Add, "New Document")
            }
        }
    ) { padding ->
        if (galleryItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "No Projects Yet",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "Create a new project to start drawing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(galleryItems) { item ->
                    GalleryItem(
                        item = item,
                        onOpen = {
                            viewModel.openDocument(item.id)
                            navController.navigate("canvas")
                        },
                        onDelete = {
                            showDeleteConfirm.value = item
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryItem(
    item: GalleryItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                .clickable(onClick = onOpen),
            contentAlignment = Alignment.Center
        ) {
            if (item.thumbPath != null) {
                AsyncImage(
                    model = item.thumbPath,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                )
            } else {
                Text(
                    "${item.width}×${item.height}",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(
                onClick = { showMenu = !showMenu },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            ) {
                Text("⋮", color = Color.White)
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                )
            }
        }

        Text(
            text = item.name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1
        )
        Text(
            text = "${item.width}×${item.height} • ${dateFormat.format(Date(item.modifiedAt))}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp),
            maxLines = 1
        )
    }
}

@Composable
fun NewDocumentDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("Untitled") }
    var width by remember { mutableStateOf("2048") }
    var height by remember { mutableStateOf("2048") }
    val presets = listOf(
        "1024×1024" to (1024 to 1024),
        "2048×2048" to (2048 to 2048),
        "4096×4096" to (4096 to 4096),
        "512×512" to (512 to 512)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors()
                )

                Text("Canvas Size:", style = MaterialTheme.typography.labelMedium)
                presets.forEach { (label, dims) ->
                    OutlinedButton(
                        onClick = {
                            width = dims.first.toString()
                            height = dims.second.toString()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(label)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = width,
                        onValueChange = { width = it },
                        label = { Text("W") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors()
                    )
                    TextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("H") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = width.toIntOrNull() ?: 2048
                    val h = height.toIntOrNull() ?: 2048
                    onCreate(name, w.coerceIn(256, 8192), h.coerceIn(256, 8192))
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
