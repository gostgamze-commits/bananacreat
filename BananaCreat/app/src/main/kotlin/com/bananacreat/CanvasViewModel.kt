package com.bananacreat

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bananacreat.data.DocumentRepository
import com.bananacreat.data.GalleryItem
import com.bananacreat.engine.BrushEngine
import com.bananacreat.engine.StrokeProcessor
import com.bananacreat.engine.UndoStack
import com.bananacreat.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CanvasViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DocumentRepository(app)
    val brushEngine = BrushEngine()
    val undoStack = UndoStack(30)

    private val _document = MutableStateFlow<ArtworkDocument?>(null)
    val document: StateFlow<ArtworkDocument?> = _document.asStateFlow()

    private val _galleryItems = MutableStateFlow<List<GalleryItem>>(emptyList())
    val galleryItems: StateFlow<List<GalleryItem>> = _galleryItems.asStateFlow()

    private val _currentBrush = MutableStateFlow(DefaultBrushes[0])
    val currentBrush: StateFlow<BrushSettings> = _currentBrush.asStateFlow()

    private val _brushColor = MutableStateFlow(Color.Black)
    val brushColor: StateFlow<Color> = _brushColor.asStateFlow()

    private val _canvasVersion = MutableStateFlow(0)
    val canvasVersion: StateFlow<Int> = _canvasVersion.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var strokeProcessor: StrokeProcessor? = null
    private var autosaveJob: Job? = null

    fun loadGallery() { viewModelScope.launch { _galleryItems.value = repo.listDocuments() } }

    fun openDocument(id: String) {
        viewModelScope.launch {
            val doc = repo.loadDocument(id)
            if (doc != null) {
                _document.value = doc
                strokeProcessor = StrokeProcessor(doc, brushEngine, undoStack)
                undoStack.clear()
                updateUndoState()
                startAutosave()
            }
        }
    }

    fun newDocument(name: String, width: Int, height: Int) {
        val doc = repo.createNewDocument(name, width, height)
        _document.value = doc
        strokeProcessor = StrokeProcessor(doc, brushEngine, undoStack)
        undoStack.clear()
        updateUndoState()
        startAutosave()
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            repo.deleteDocument(id)
            _galleryItems.value = repo.listDocuments()
        }
    }

    fun setBrush(brush: BrushSettings) { _currentBrush.value = brush }
    fun setBrushColor(color: Color) { _brushColor.value = color }
    fun setBrushSize(size: Float) { _currentBrush.value = _currentBrush.value.copy(size = size) }
    fun setBrushOpacity(opacity: Float) { _currentBrush.value = _currentBrush.value.copy(opacity = opacity) }

    fun beginStroke(x: Float, y: Float, pressure: Float) {
        strokeProcessor?.beginStroke(x, y, pressure, _brushColor.value, _currentBrush.value)
        invalidateCanvas()
    }

    fun continueStroke(x: Float, y: Float, pressure: Float) {
        strokeProcessor?.continueStroke(x, y, pressure, _brushColor.value, _currentBrush.value)
        invalidateCanvas()
    }

    fun endStroke() {
        strokeProcessor?.endStroke()
        updateUndoState()
    }

    fun undo() {
        val doc = _document.value ?: return
        if (undoStack.undo(doc)) { invalidateCanvas(); updateUndoState() }
    }

    fun redo() {
        val doc = _document.value ?: return
        if (undoStack.redo(doc)) { invalidateCanvas(); updateUndoState() }
    }

    fun fillLayer(color: Color) {
        strokeProcessor?.fillLayer(color)
        invalidateCanvas()
        updateUndoState()
    }

    fun clearLayer() {
        strokeProcessor?.clearLayer()
        invalidateCanvas()
        updateUndoState()
    }

    fun floodFill(x: Float, y: Float) {
        strokeProcessor?.floodFill(x, y, _brushColor.value)
        invalidateCanvas()
        updateUndoState()
    }

    fun setActiveLayer(index: Int) {
        _document.value?.let { it.activeLayerIndex = index }
        invalidateCanvas()
    }

    fun addLayer() {
        _document.value?.addLayer()
        invalidateCanvas()
    }

    fun deleteLayer(index: Int) {
        _document.value?.deleteLayer(index)
        invalidateCanvas()
    }

    fun moveLayer(from: Int, to: Int) {
        _document.value?.moveLayer(from, to)
        invalidateCanvas()
    }

    fun mergeDown(index: Int) {
        _document.value?.mergeDown(index)
        invalidateCanvas()
    }

    fun setLayerVisible(index: Int, visible: Boolean) {
        _document.value?.layers?.getOrNull(index)?.isVisible = visible
        invalidateCanvas()
    }

    fun setLayerOpacity(index: Int, opacity: Float) {
        _document.value?.layers?.getOrNull(index)?.opacity = opacity
        invalidateCanvas()
    }

    fun setLayerBlendMode(index: Int, mode: LayerBlendMode) {
        _document.value?.layers?.getOrNull(index)?.blendMode = mode
        invalidateCanvas()
    }

    fun setLayerName(index: Int, name: String) {
        _document.value?.layers?.getOrNull(index)?.name = name
    }

    fun exportPng(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = _document.value ?: return@launch
            val bmp = doc.composite()
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bmp.recycle()
        }
    }

    fun exportJpeg(uri: Uri, quality: Int = 95) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = _document.value ?: return@launch
            val bmp = doc.composite()
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                bmp.compress(Bitmap.CompressFormat.JPEG, quality, it)
            }
            bmp.recycle()
        }
    }

    fun saveDocument() {
        viewModelScope.launch {
            _document.value?.let { repo.saveDocument(it) }
        }
    }

    private fun startAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                _document.value?.let { repo.saveDocument(it) }
            }
        }
    }

    private fun invalidateCanvas() { _canvasVersion.value++ }
    private fun updateUndoState() {
        _canUndo.value = undoStack.canUndo
        _canRedo.value = undoStack.canRedo
    }

    override fun onCleared() {
        super.onCleared()
        brushEngine.clearCache()
        autosaveJob?.cancel()
    }
}
