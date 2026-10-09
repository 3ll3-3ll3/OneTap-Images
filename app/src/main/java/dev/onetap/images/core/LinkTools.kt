package dev.onetap.images.core

/** Pure Kotlin URL tools, independent from Android framework. */
object LinkTools {
    private val urlPattern = Regex("https?://[^\\s<>\\\"'，。；、]+", RegexOption.IGNORE_CASE)
    fun firstUrl(text: String): String? =
        urlPattern.find(text)?.value?.trimEnd(')', ']', '.', ',', '！', '。')

    fun platform(raw: String): Platform? {
        val host = Regex("^https?://([^/:?#]+)", RegexOption.IGNORE_CASE)
            .find(raw)?.groupValues?.get(1)?.lowercase()?.removePrefix("www.") ?: return null
        return when {
            host == "xiaohongshu.com" || host.endsWith(".xiaohongshu.com") || host == "xhslink.com" -> Platform.XHS
            host == "douyin.com" || host.endsWith(".douyin.com") || host == "iesdouyin.com" -> Platform.DOUYIN
            host == "x.com" || host.endsWith(".x.com") || host == "twitter.com" || host.endsWith(".twitter.com") -> Platform.X
            host == "threads.net" || host.endsWith(".threads.net") || host == "threads.com" || host.endsWith(".threads.com") -> Platform.THREADS
            else -> null
        }
    }
    fun xStatusId(url: String): String? =
        Regex("/(?:status|statuses)/(\\d{5,22})(?:[/?#]|$)").find(url)?.groupValues?.get(1)
    fun safeId(input: String): String =
        input.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(72).ifEmpty { "unknown" }
}
