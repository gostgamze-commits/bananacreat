package com.bananacreat.model

import android.graphics.Bitmap
import java.util.UUID

data class AnimationProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Animation",
    val width: Int = 2048,
    val height: Int = 2048,
    val fps: Int = 24,
    val frames: MutableList<AnimationFrame> = mutableListOf(),
    var currentFrameIndex: Int = 0,
    var createdAt: Long = System.currentTimeMillis(),
    var modifiedAt: Long = System.currentTimeMillis()
) {
    fun currentFrame(): AnimationFrame? = frames.getOrNull(currentFrameIndex)

    fun addFrame(bitmap: Bitmap? = null): AnimationFrame {
        val bmp = bitmap ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val frame = AnimationFrame(
            bitmap = bmp,
            duration = (1000 / fps),
            index = frames.size
        )
        frames.add(frame)
        return frame
    }

    fun duplicateFrame(index: Int) {
        val source = frames.getOrNull(index) ?: return
        val copy = Bitmap.createBitmap(source.bitmap)
        val newFrame = AnimationFrame(
            bitmap = copy,
            duration = source.duration,
            index = index + 1
        )
        frames.add(index + 1, newFrame)
    }

    fun deleteFrame(index: Int) {
        if (frames.size <= 1) return
        frames[index].bitmap.recycle()
        frames.removeAt(index)
        currentFrameIndex = currentFrameIndex.coerceIn(0, frames.size - 1)
    }

    fun moveFrame(from: Int, to: Int) {
        if (from == to || from < 0 || to < 0 || from >= frames.size || to >= frames.size) return
        val frame = frames.removeAt(from)
        frames.add(to, frame)
    }

    fun getTotalDuration(): Long {
        return frames.sumOf { it.duration.toLong() }
    }
}
