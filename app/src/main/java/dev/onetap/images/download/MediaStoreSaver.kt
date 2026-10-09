package dev.onetap.images.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.graphics.BitmapFactory
import dev.onetap.images.core.LinkTools
import dev.onetap.images.core.ImagePost
import java.io.File
import java.util.Locale

/** Publish only after fully downloading and verifying. Delete IS_PENDING rows on error. */
class MediaStoreSaver(private val context: Context) {
    fun isPresent(uri: String): Boolean = try {
        context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { it.statSize > 0 } ?: false
    } catch (_: Exception) { false }

    fun save(post: ImagePost, index: Int, file: File, mime: String): String {
        require(mime in VALID_MIME) { "不支持的图片格式：$mime" }
        require(file.length() in 1..(50L * 1024 * 1024)) { "图片文件长度无效或过大" }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法解码图片，已拒绝保存" }
        val ext = when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic" -> "heic"
            "image/heif" -> "heif"
            else -> "jpg"
        }
        val folder = currentFolder
        val name = "${post.platform.prefix}_${LinkTools.safeId(post.postId)}_${(index + 1).toString().padStart(3,'0')}.$ext"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "$folder/")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val inserted = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore 无法创建相册文件")
        try {
            resolver.openOutputStream(inserted, "w")?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            } ?: error("MediaStore 无法写入图片")
            val updated = resolver.update(inserted,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            require(updated > 0) { "MediaStore 无法公开图片" }
            return inserted.toString()
        } catch (e: Exception) {
            resolver.delete(inserted, null, null)
            throw e
        }
    }

    var currentFolder: String = "Pictures/OneTap Images"

    companion object {
        val VALID_MIME = setOf("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif")
        fun mimeFromHeader(raw: String?): String? = when (raw?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)) {
            "image/jpeg", "image/jpg" -> "image/jpeg"
            "image/png" -> "image/png"
            "image/webp" -> "image/webp"
            "image/heic" -> "image/heic"
            "image/heif" -> "image/heif"
            else -> null
        }
    }
}
