package com.bananacreat.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import com.bananacreat.model.ArtworkDocument

class UndoStack(private val maxSteps: Int = 30) {
    private val undoStack = ArrayDeque<UndoEntry>()
    private val redoStack = ArrayDeque<UndoEntry>()

    data class UndoEntry(
        val layerIndex: Int,
        val snapshot: Bitmap
    )

    fun push(doc: ArtworkDocument) {
        val layer = doc.activeLayer() ?: return
        val snap = layer.bitmap.copy(Bitmap.Config.ARGB_8888, false)
        undoStack.addLast(UndoEntry(doc.activeLayerIndex, snap))
        if (undoStack.size > maxSteps) {
            undoStack.removeFirst().snapshot.recycle()
        }
        redoStack.forEach { it.snapshot.recycle() }
        redoStack.clear()
    }

    fun undo(doc: ArtworkDocument): Boolean {
        if (undoStack.isEmpty()) return false
        val entry = undoStack.removeLast()

        val layer = doc.layers.getOrNull(entry.layerIndex) ?: return false
        val redoSnap = layer.bitmap.copy(Bitmap.Config.ARGB_8888, false)
        redoStack.addLast(UndoEntry(entry.layerIndex, redoSnap))

        val canvas = Canvas(layer.bitmap)
        canvas.drawBitmap(entry.snapshot, 0f, 0f, null)
        entry.snapshot.recycle()
        doc.activeLayerIndex = entry.layerIndex
        return true
    }

    fun redo(doc: ArtworkDocument): Boolean {
        if (redoStack.isEmpty()) return false
        val entry = redoStack.removeLast()

        val layer = doc.layers.getOrNull(entry.layerIndex) ?: return false
        val undoSnap = layer.bitmap.copy(Bitmap.Config.ARGB_8888, false)
        undoStack.addLast(UndoEntry(entry.layerIndex, undoSnap))

        val canvas = Canvas(layer.bitmap)
        canvas.drawBitmap(entry.snapshot, 0f, 0f, null)
        entry.snapshot.recycle()
        doc.activeLayerIndex = entry.layerIndex
        return true
    }

    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    fun clear() {
        undoStack.forEach { it.snapshot.recycle() }
        undoStack.clear()
        redoStack.forEach { it.snapshot.recycle() }
        redoStack.clear()
    }
}
