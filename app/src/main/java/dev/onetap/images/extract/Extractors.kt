package dev.onetap.images.extract

import dev.onetap.images.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

interface PostImageExtractor {
    val platform: Platform
    suspend fun extract(url: String): Extraction
}

class UnavailableExtractor(override val platform: Platform, private val detail: String) : PostImageExtractor {
    override suspend fun extract(url: String): Extraction = Extraction.Failure("${platform.label}：$detail")
}

/** Unofficial public embedding endpoint; may be unavailable/rate limited. Not authenticated. */
class XExtractor(private val client: OkHttpClient) : PostImageExtractor {
    override val platform = Platform.X
    override suspend fun extract(url: String): Extraction = withContext(Dispatchers.IO) {
        val id = LinkTools.xStatusId(url) ?: return@withContext Extraction.Failure("找不到 X 帖子的 status ID")
        try {
            val request = Request.Builder().url("https://cdn.syndication.twimg.com/tweet-result?id=$id&token=0")
                .header("Accept", "application/json").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Extraction.Failure(
                    "X 公开嵌入数据不可访问：HTTP ${response.code}（可能需要授权或内容已删除）"
                )
                val text = response.body?.string()?.take(2_000_000) ?: ""
                XMediaParser.parse(id, text)
            }
        } catch (e: Exception) {
            Extraction.Failure("X 图片解析失败：${e.localizedMessage ?: "网络或 JSON 不可用"}")
        }
    }
}

class ExtractorRegistry {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    val downloadClient: OkHttpClient get() = client
    private val extractors = listOf(
        UnavailableExtractor(Platform.XHS, "当前没有已验证的公开全量轮播解析方法，本版本不会假装支持。"),
        UnavailableExtractor(Platform.DOUYIN, "图文列表接口尚未完成验证，不会用视频封面代替。"),
        XExtractor(client),
        UnavailableExtractor(Platform.THREADS, "尚无已验证的公开轮播解析方式，官方 API 可能要求授权。")
    )
    suspend fun extractFromText(text: String): Extraction {
        if (text.trim() == "onetap://demo/album") return demo()
        val url = LinkTools.firstUrl(text) ?: return Extraction.Failure("没有找到 http(s) 帖子链接")
        val platform = LinkTools.platform(url) ?: return Extraction.Failure("暂不支持这个网站的帖子链接")
        return extractors.first { it.platform == platform }.extract(url)
    }
    fun demo(): Extraction = Extraction.Success(
        ImagePost(Platform.DEMO, "sample-001", (1..3).map { i ->
            ImageAsset("$i", "https://placehold.co/800x1200/jpeg?text=OneTap+Sample+$i")
        })
    )
}
