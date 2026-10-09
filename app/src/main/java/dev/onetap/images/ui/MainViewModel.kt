package dev.onetap.images.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.onetap.images.core.*
import dev.onetap.images.data.AppSettings
import dev.onetap.images.data.Preferences
import dev.onetap.images.download.ImageDownloadManager
import dev.onetap.images.extract.ExtractorRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScreenState(
    val input: String = "",
    val post: ImagePost? = null,
    val items: List<DownloadItem> = emptyList(),
    val message: String = "从分享菜单选择「一键存图」，或粘贴帖子链接。",
    val busy: Boolean = false,
    val downloading: Boolean = false,
    val lastSavedUri: String? = null,
    val history: List<String> = emptyList(),
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = AppSettings(app)
    private val registry = ExtractorRegistry()
    private val downloader = ImageDownloadManager(app, registry.downloadClient, storage)
    private var work: Job? = null
    private val _state = MutableStateFlow(ScreenState())
    val state: StateFlow<ScreenState> = _state.asStateFlow()
    val prefs: StateFlow<Preferences> = storage.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, Preferences())

    init { reloadHistory() }

    fun setInput(input: String) { _state.value = _state.value.copy(input = input) }

    fun parse(auto: Boolean = false) {
        if (_state.value.downloading || _state.value.busy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, post = null, items = emptyList(), message = "正在解析帖子……")
            val result = registry.extractFromText(_state.value.input)
            when (result) {
                is Extraction.Success -> {
                    _state.value = _state.value.copy(
                        busy = false, post = result.post,
                        items = result.post.images.map { DownloadItem(it) },
                        message = "已识别 ${result.post.images.size} 张静态图片"
                    )
                    if (auto) download()
                }
                is Extraction.Failure -> _state.value = _state.value.copy(busy = false, message = result.reason)
            }
        }
    }

    fun previewDemo() {
        if (_state.value.downloading) return
        setInput("onetap://demo/album")
        parse(false)
    }

    fun shareUris(uris: List<Uri>) {
        if (_state.value.downloading || uris.isEmpty()) return
        val images = uris.distinct().mapIndexed { index, uri -> ImageAsset(index.toString(), uri.toString(), true) }
        val post = ImagePost(Platform.SHARED, "${System.currentTimeMillis()}", images)
        _state.value = _state.value.copy(
            post = post, items = images.map { DownloadItem(it) },
            message = "收到 ${images.size} 张分享的图片", input = ""
        )
        if (prefs.value.autoDownload) download()
    }

    fun shareText(text: String) {
        if (_state.value.downloading) return
        setInput(text)
        parse(prefs.value.autoDownload)
    }

    fun download() {
        val post = _state.value.post ?: return
        if (_state.value.downloading || _state.value.busy) return
        work = viewModelScope.launch {
            _state.value = _state.value.copy(downloading = true, message = "正在下载……")
            try {
                downloader.run(post, _state.value.items, storage.snapshot()) { index, status, error ->
                    val current = _state.value
                    val updated = current.items.toMutableList()
                    if (index in updated.indices) {
                        updated[index] = updated[index].copy(state = status, error = error)
                        _state.value = current.copy(items = updated,
                            lastSavedUri = if (status == ItemState.SAVED) error else current.lastSavedUri)
                    }
                }
                val items = _state.value.items
                val saved = items.count { it.state == ItemState.SAVED }
                val skipped = items.count { it.state == ItemState.SKIPPED }
                val failed = items.count { it.state == ItemState.FAILED }
                _state.value = _state.value.copy(message = "完成：新保存 $saved 张，跳过 $skipped 张，失败 $failed 张")
                reloadHistory()
            } catch (_: CancellationException) {
                _state.value = _state.value.copy(message = "任务已取消，已完成的图片仍保留")
            } finally { _state.value = _state.value.copy(downloading = false) }
        }
    }

    fun cancel() { work?.cancel() }
    fun retryFailed() {
        if (_state.value.downloading) return
        _state.value = _state.value.copy(items = _state.value.items.map {
            if (it.state == ItemState.FAILED) it.copy(state = ItemState.PENDING, error = "") else it
        })
        download()
    }

    fun updatePrefs(value: Preferences) { viewModelScope.launch { storage.update(value) } }
    fun reloadHistory() { viewModelScope.launch {
        val all = storage.records()
        _state.value = _state.value.copy(history = all.map { it.key.substringBeforeLast(':') }.distinct().take(100))
    } }
}
