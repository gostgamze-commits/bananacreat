package com.bananacreat.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.bananacreat.model.AnimationProject
import com.bananacreat.model.AnimationFrame
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AnimationRepository(private val context: Context) {
    private val gson = Gson()
    private val animDir get() = File(context.filesDir, "animations").also { it.mkdirs() }

    suspend fun saveAnimation(proj: AnimationProject): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(animDir, proj.id).also { it.mkdirs() }
            val meta = AnimationMeta(
                id = proj.id,
                name = proj.name,
                width = proj.width,
                height = proj.height,
                fps = proj.fps,
                frameCount = proj.frames.size,
                createdAt = proj.createdAt,
                modifiedAt = System.currentTimeMillis()
            )
            File(dir, "meta.json").writeText(gson.toJson(meta))
            proj.frames.forEachIndexed { i, frame ->
                val bmpFile = File(dir, "frame_$i.png")
                FileOutputStream(bmpFile).use { frame.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loadAnimation(id: String): AnimationProject? = withContext(Dispatchers.IO) {
        try {
            val dir = File(animDir, id)
            val meta = gson.fromJson(File(dir, "meta.json").readText(), AnimationMeta::class.java)
            val frames = (0 until meta.frameCount).mapNotNull { i ->
                val bmpFile = File(dir, "frame_$i.png")
                if (!bmpFile.exists()) return@mapNotNull null
                val bmp = BitmapFactory.decodeFile(bmpFile.absolutePath)?.copy(Bitmap.Config.ARGB_8888, true) ?: return@mapNotNull null
                AnimationFrame(bitmap = bmp, duration = 1000 / meta.fps, index = i)
            }.toMutableList()

            if (frames.isEmpty()) return@withContext null

            AnimationProject(
                id = meta.id,
                name = meta.name,
                width = meta.width,
                height = meta.height,
                fps = meta.fps,
                frames = frames,
                createdAt = meta.createdAt,
                modifiedAt = meta.modifiedAt
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun listAnimations(): List<GalleryItem> = withContext(Dispatchers.IO) {
        animDir.listFiles()?.mapNotNull { dir ->
            try {
                val meta = gson.fromJson(File(dir, "meta.json").readText(), AnimationMeta::class.java)
                GalleryItem(
                    id = meta.id,
                    name = meta.name,
                    width = meta.width,
                    height = meta.height,
                    modifiedAt = meta.modifiedAt,
                    thumbPath = null,
                    isAnimation = true
                )
            } catch (e: Exception) { null }
        }?.sortedByDescending { it.modifiedAt } ?: emptyList()
    }

    suspend fun deleteAnimation(id: String) = withContext(Dispatchers.IO) {
        File(animDir, id).deleteRecursively()
    }

    fun createNewAnimation(name: String, width: Int, height: Int, fps: Int = 24): AnimationProject {
        val proj = AnimationProject(name = name, width = width, height = height, fps = fps)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        proj.addFrame(bmp)
        return proj
    }
}

data class AnimationMeta(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val frameCount: Int,
    val createdAt: Long,
    val modifiedAt: Long
)
