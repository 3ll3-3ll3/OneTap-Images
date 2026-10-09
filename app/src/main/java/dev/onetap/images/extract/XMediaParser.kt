package dev.onetap.images.extract

import dev.onetap.images.core.*
import org.json.JSONObject

/** Parses only the requested tweet's top-level media. Never traverses quoted/reply posts. */
object XMediaParser {
    fun parse(requestedId: String, text: String): Extraction {
        val json = JSONObject(text)
        val returnedId = json.optString("id_str").ifBlank { json.optString("id") }
        if (returnedId != requestedId) return Extraction.Failure("响应帖子 ID 不匹配，已拒绝下载")
        val arr = json.optJSONArray("mediaDetails")
        val images = buildList {
            if (arr != null) for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                if (item.optString("type") != "photo") continue
                val media = item.optString("media_url_https")
                if (!media.startsWith("https://pbs.twimg.com/media/")) continue
                val stable = item.optString("id_str").ifBlank { i.toString() }
                val base = media.substringBefore('?')
                val extension = base.substringAfterLast('.', "jpg").lowercase()
                val format = if (extension in setOf("jpg", "jpeg", "png", "webp")) extension else "jpg"
                add(ImageAsset(stable, "$base?format=$format&name=orig"))
            }
        }.distinctBy { it.key }
        return if (images.isEmpty()) Extraction.Failure("帖子未提供可访问的静态图片，不会下载视频封面")
        else Extraction.Success(ImagePost(Platform.X, requestedId, images))
    }
}
