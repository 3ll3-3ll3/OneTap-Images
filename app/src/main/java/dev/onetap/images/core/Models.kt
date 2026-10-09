package dev.onetap.images.core

enum class Platform(val label: String, val prefix: String) {
    XHS("小红书", "XHS"), DOUYIN("抖音", "DY"), X("X", "X"), THREADS("Threads", "TH"),
    SHARED("系统分享", "SHARED"), DEMO("演示图片", "DEMO")
}

data class ImageAsset(val key: String, val source: String, val local: Boolean = false)
data class ImagePost(val platform: Platform, val postId: String, val images: List<ImageAsset>)
sealed interface Extraction {
    data class Success(val post: ImagePost) : Extraction
    data class Failure(val reason: String) : Extraction
}
enum class ItemState { PENDING, DOWNLOADING, SAVED, SKIPPED, FAILED }
data class DownloadItem(val image: ImageAsset, val state: ItemState = ItemState.PENDING, val error: String = "")
data class RunSummary(val saved: Int, val skipped: Int, val failed: Int)
