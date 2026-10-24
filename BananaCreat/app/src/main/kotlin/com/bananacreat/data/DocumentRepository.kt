package com.bananacreat.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.bananacreat.model.ArtworkDocument
import com.bananacreat.model.Layer
import com.bananacreat.model.LayerBlendMode
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DocumentRepository(private val context: Context) {
    private val gson = Gson()
    private val docsDir get() = File(context.filesDir, "artworks").also { it.mkdirs() }

    suspend fun saveDocument(doc: ArtworkDocument): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(docsDir, doc.id).also { it.mkdirs() }
            val meta = DocumentMeta(
                id = doc.id,
                name = doc.name,
                width = doc.width,
                height = doc.height,
                activeLayerIndex = doc.activeLayerIndex,
                createdAt = doc.createdAt,
                modifiedAt = System.currentTimeMillis(),
                layers = doc.layers.map { LayerMeta(it.id, it.name, it.isVisible, it.opacity, it.blendMode.name, it.isLocked) }
            )
            File(dir, "meta.json").writeText(gson.toJson(meta))
            doc.layers.forEachIndexed { i, layer ->
                val bmpFile = File(dir, "layer_${layer.id}.png")
                FileOutputStream(bmpFile).use { layer.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            // Save thumbnail
            val thumb = Bitmap.createScaledBitmap(doc.composite(), 256, 256, true)
            FileOutputStream(File(dir, "thumb.jpg")).use { thumb.compress(Bitmap.CompressFormat.JPEG, 80, it) }
            thumb.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loadDocument(id: String): ArtworkDocument? = withContext(Dispatchers.IO) {
        try {
            val dir = File(docsDir, id)
            val meta = gson.fromJson(File(dir, "meta.json").readText(), DocumentMeta::class.java)
            val layers = meta.layers.mapNotNull { lm ->
                val bmpFile = File(dir, "layer_${lm.id}.png")
                if (!bmpFile.exists()) return@mapNotNull null
                val bmp = BitmapFactory.decodeFile(bmpFile.absolutePath)?.copy(Bitmap.Config.ARGB_8888, true) ?: return@mapNotNull null
                Layer(
                    id = lm.id,
                    name = lm.name,
                    bitmap = bmp,
                    isVisible = lm.isVisible,
                    opacity = lm.opacity,
                    blendMode = try { LayerBlendMode.valueOf(lm.blendMode) } catch (e: Exception) { LayerBlendMode.NORMAL },
                    isLocked = lm.isLocked
                )
            }.toMutableList()

            if (layers.isEmpty()) return@withContext null

            ArtworkDocument(
                id = meta.id,
                name = meta.name,
                width = meta.width,
                height = meta.height,
                layers = layers,
                activeLayerIndex = meta.activeLayerIndex.coerceIn(0, layers.size - 1),
                createdAt = meta.createdAt,
                modifiedAt = meta.modifiedAt
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun listDocuments(): List<GalleryItem> = withContext(Dispatchers.IO) {
        docsDir.listFiles()?.mapNotNull { dir ->
            try {
                val meta = gson.fromJson(File(dir, "meta.json").readText(), DocumentMeta::class.java)
                val thumbFile = File(dir, "thumb.jpg")
                GalleryItem(
                    id = meta.id,
                    name = meta.name,
                    width = meta.width,
                    height = meta.height,
                    modifiedAt = meta.modifiedAt,
                    thumbPath = if (thumbFile.exists()) thumbFile.absolutePath else null
                )
            } catch (e: Exception) { null }
        }?.sortedByDescending { it.modifiedAt } ?: emptyList()
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        File(docsDir, id).deleteRecursively()
    }

    fun createNewDocument(name: String, width: Int, height: Int): ArtworkDocument {
        val doc = ArtworkDocument(name = name, width = width, height = height)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        doc.layers.add(Layer(name = "Background", bitmap = bmp))
        return doc
    }
}

data class GalleryItem(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val modifiedAt: Long,
    val thumbPath: String?
)

data class DocumentMeta(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val activeLayerIndex: Int,
    val createdAt: Long,
    val modifiedAt: Long,
    val layers: List<LayerMeta>
)

data class LayerMeta(
    val id: String,
    val name: String,
    val isVisible: Boolean,
    val opacity: Float,
    val blendMode: String,
    val isLocked: Boolean
)
