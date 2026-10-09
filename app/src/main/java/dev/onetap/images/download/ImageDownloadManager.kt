package dev.onetap.images.download

import android.content.Context
import android.net.Uri
import dev.onetap.images.core.*
import dev.onetap.images.data.AppSettings
import dev.onetap.images.data.Preferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

class ImageDownloadManager(
    private val context: Context,
    private val client: OkHttpClient,
    private val settings: AppSettings,
) {
    private val saver = MediaStoreSaver(context)

    suspend fun run(
        post: ImagePost,
        current: List<DownloadItem>,
        prefs: Preferences,
        onState: (Int, ItemState, String) -> Unit,
    ): RunSummary = coroutineScope {
        saver.currentFolder = prefs.folder
        val old = if (prefs.skipExisting) settings.records().associateBy { it.key } else emptyMap()
        val semaphore = Semaphore(prefs.concurrency)
        val tasks = current.mapIndexedNotNull { index, item ->
            if (item.state == ItemState.SAVED || item.state == ItemState.SKIPPED) null
            else async {
                semaphore.withPermit {
                    val uniqueKey = "${post.platform.name}:${post.postId}:${item.image.key}"
                    if (prefs.skipExisting &&
                        old[uniqueKey]?.let { withContext(Dispatchers.IO) { saver.isPresent(it.uri) } } == true) {
                        onState(index, ItemState.SKIPPED, "已保存，跳过")
                        return@withPermit
                    }
                    onState(index, ItemState.DOWNLOADING, "")
                    try {
                        val (tmp, mime) = loadToCache(item.image)
                        try {
                            val uri = withContext(Dispatchers.IO) { saver.save(post, index, tmp, mime) }
                            settings.add(uniqueKey, uri)
                            onState(index, ItemState.SAVED, uri)
                        } finally { tmp.delete() }
                    } catch (cancel: CancellationException) {
                        onState(index, ItemState.PENDING, "已取消")
                        throw cancel
                    } catch (e: Exception) {
                        onState(index, ItemState.FAILED, e.localizedMessage ?: "下载失败")
                    }
                }
            }
        }
        tasks.awaitAll()
        RunSummary(0, 0, 0) // UI aggregates per-item results
    }

    private suspend fun loadToCache(image: ImageAsset): Pair<File, String> = withContext(Dispatchers.IO) {
        val file = File.createTempFile("onetap_", ".partial", context.cacheDir)
        try {
            var mime: String? = null
            if (image.local) {
                val uri = Uri.parse(image.source)
                require(uri.scheme == "content") { "只能保存由系统分享的 content URI" }
                mime = MediaStoreSaver.mimeFromHeader(context.contentResolver.getType(uri))
                    ?: error("分享的媒体不是支持的静态图片")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> copyLimited(input, output) }
                } ?: error("无法读取分享的图片")
            } else {
                require(image.source.startsWith("https://")) { "只接受 HTTPS 图片链接" }
                val request = Request.Builder().url(image.source).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("图片服务器 HTTP ${response.code}")
                    require(response.request.url.isHttps) { "重定向不安全" }
                    mime = MediaStoreSaver.mimeFromHeader(response.header("Content-Type"))
                        ?: error("服务器没有返回支持的静态图片 MIME 类型")
                    response.body?.byteStream()?.use { input ->
                        file.outputStream().use { output -> copyLimited(input, output) }
                    } ?: error("图片响应为空")
                }
            }
            file to (mime ?: error("无法判定图片格式"))
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    private fun copyLimited(input: java.io.InputStream, output: java.io.OutputStream) {
        val buffer = ByteArray(16_384)
        var total = 0L
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            if (total > 50L * 1024 * 1024) throw IOException("单张图片不得超过 50MB")
            output.write(buffer, 0, n)
        }
    }
}
