package com.bananacreat

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bananacreat.data.AnimationRepository
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

class AnimationViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = DocumentRepository(app)
    private val animRepo = AnimationRepository(app)
    val brushEngine = BrushEngine()
    val undoStack = UndoStack(30)

    private val _animationProject = MutableStateFlow<AnimationProject?>(null)
    val animationProject: StateFlow<AnimationProject?> = _animationProject.asStateFlow()

    private val _currentFrameIndex = MutableStateFlow(0)
    val currentFrameIndex: StateFlow<Int> = _currentFrameIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackTime = MutableStateFlow(0L)
    val playbackTime: StateFlow<Long> = _playbackTime.asStateFlow()

    private val _currentBrush = MutableStateFlow(DefaultBrushes[0])
    val currentBrush: StateFlow<BrushSettings> = _currentBrush.asStateFlow()

    private val _brushColor = MutableStateFlow(Color.Black)
    val brushColor: StateFlow<Color> = _brushColor.asStateFlow()

    private val _canvasVersion = MutableStateFlow(0)
    val canvasVersion: StateFlow<Int> = _canvasVersion.asStateFlow()

    private val _onionSkinOpacity = MutableStateFlow(0.3f)
    val onionSkinOpacity: StateFlow<Float> = _onionSkinOpacity.asStateFlow()

    private val _showOnionSkin = MutableStateFlow(true)
    val showOnionSkin: StateFlow<Boolean> = _showOnionSkin.asStateFlow()

    private var strokeProcessor: StrokeProcessor? = null
    private var autosaveJob: Job? = null
    private var playbackJob: Job? = null

    fun newAnimation(name: String, width: Int, height: Int, fps: Int = 24) {
        val proj = animRepo.createNewAnimation(name, width, height, fps)
        _animationProject.value = proj
        strokeProcessor = StrokeProcessor(
            ArtworkDocument(width = width, height = height, layers = mutableListOf(
                Layer(name = "Frame 1", bitmap = proj.frames[0].bitmap)
            )),
            brushEngine,
            undoStack
        )
        undoStack.clear()
        startAutosave()
    }

    fun openAnimation(id: String) {
        viewModelScope.launch {
            val proj = animRepo.loadAnimation(id)
            if (proj != null) {
                _animationProject.value = proj
                _currentFrameIndex.value = 0
                strokeProcessor = StrokeProcessor(
                    ArtworkDocument(width = proj.width, height = proj.height, layers = mutableListOf(
                        Layer(name = "Current Frame", bitmap = proj.currentFrame()?.bitmap ?: Bitmap.createBitmap(proj.width, proj.height, Bitmap.Config.ARGB_8888))
                    )),
                    brushEngine,
                    undoStack
                )
                undoStack.clear()
                startAutosave()
            }
        }
    }

    fun addFrame(duplicateFrom: Int? = null) {
        val proj = _animationProject.value ?: return
        if (duplicateFrom != null) {
            proj.duplicateFrame(duplicateFrom)
        } else {
            proj.addFrame()
        }
        _currentFrameIndex.value = proj.frames.size - 1
        invalidateCanvas()
    }

    fun deleteFrame(index: Int) {
        _animationProject.value?.deleteFrame(index)
        invalidateCanvas()
    }

    fun moveFrame(from: Int, to: Int) {
        _animationProject.value?.moveFrame(from, to)
        invalidateCanvas()
    }

    fun selectFrame(index: Int) {
        val proj = _animationProject.value ?: return
        if (index in proj.frames.indices) {
            _currentFrameIndex.value = index
            invalidateCanvas()
        }
    }

    fun playAnimation() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        playbackJob = viewModelScope.launch {
            val proj = _animationProject.value ?: return@launch
            while (_isPlaying.value) {
                for (i in proj.frames.indices) {
                    _currentFrameIndex.value = i
                    _playbackTime.value = proj.frames.subList(0, i).sumOf { it.duration.toLong() }
                    delay(proj.frames[i].duration.toLong())
                    if (!_isPlaying.value) break
                }
            }
        }
    }

    fun pauseAnimation() {
        _isPlaying.value = false
    }

    fun stopAnimation() {
        _isPlaying.value = false
        _currentFrameIndex.value = 0
        _playbackTime.value = 0L
    }

    fun scrubToFrame(index: Int) {
        pauseAnimation()
        selectFrame(index)
        val proj = _animationProject.value ?: return
        _playbackTime.value = proj.frames.subList(0, index).sumOf { it.duration.toLong() }
    }

    fun setFrameDuration(index: Int, durationMs: Int) {
        _animationProject.value?.frames?.getOrNull(index)?.duration = durationMs
    }

    fun setFPS(fps: Int) {
        val proj = _animationProject.value ?: return
        proj.fps = fps
        proj.frames.forEach { it.duration = 1000 / fps }
    }

    fun setOnionSkinOpacity(opacity: Float) {
        _onionSkinOpacity.value = opacity.coerceIn(0f, 1f)
    }

    fun toggleOnionSkin() {
        _showOnionSkin.value = !_showOnionSkin.value
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
    }

    fun saveAnimation() {
        viewModelScope.launch {
            _animationProject.value?.let { animRepo.saveAnimation(it) }
        }
    }

    private fun startAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                _animationProject.value?.let { animRepo.saveAnimation(it) }
            }
        }
    }

    private fun invalidateCanvas() { _canvasVersion.value++ }

    override fun onCleared() {
        super.onCleared()
        brushEngine.clearCache()
        autosaveJob?.cancel()
        playbackJob?.cancel()
    }
}
