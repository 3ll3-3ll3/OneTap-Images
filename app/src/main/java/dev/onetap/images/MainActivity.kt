package dev.onetap.images

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.onetap.images.core.ItemState
import dev.onetap.images.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receive(intent)
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            val preferences by vm.prefs.collectAsStateWithLifecycle()
            var tab by remember { mutableIntStateOf(0) }
            MaterialTheme(
                colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme())
                    darkColorScheme() else lightColorScheme()
            ) {
                Scaffold(
                    topBar = {
                        Text("一键存图 · OneTap Images",
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.titleLarge)
                    },
                    bottomBar = {
                        NavigationBar {
                            listOf("首页", "历史", "设置").forEachIndexed { index, name ->
                                NavigationBarItem(
                                    selected = tab == index, onClick = { tab = index },
                                    icon = { Text(when (index) { 0 -> "⌂"; 1 -> "▤"; else -> "⚙" }) },
                                    label = { Text(name) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    when (tab) {
                        0 -> Column(
                            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            OutlinedTextField(
                                value = state.input, onValueChange = vm::setInput,
                                label = { Text("帖子分享链接 / 分享文案") },
                                minLines = 2, maxLines = 5, modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = vm::parse, enabled = !state.busy && !state.downloading) {
                                    Text("识别链接")
                                }
                                OutlinedButton(onClick = {
                                    val board = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    vm.setInput(board.primaryClip?.getItemAt(0)
                                        ?.coerceToText(this@MainActivity)?.toString().orEmpty())
                                }) { Text("粘贴") }
                            }
                            Text(state.message, style = MaterialTheme.typography.bodyMedium)
                            if (state.post != null) {
                                Text("${state.post!!.platform.label} · ${state.items.size} 张图片",
                                    style = MaterialTheme.typography.titleMedium)
                                val done = state.items.count {
                                    it.state in listOf(ItemState.SAVED, ItemState.SKIPPED, ItemState.FAILED)
                                }
                                LinearProgressIndicator(
                                    progress = { if (state.items.isEmpty()) 0f else done.toFloat() / state.items.size },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("进度：$done / ${state.items.size}；成功 ${state.items.count { it.state == ItemState.SAVED }}，失败 ${state.items.count { it.state == ItemState.FAILED }}")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = vm::download, enabled = !state.downloading && !state.busy) {
                                        Text("全部下载")
                                    }
                                    OutlinedButton(onClick = vm::cancel, enabled = state.downloading) {
                                        Text("取消")
                                    }
                                }
                                if (state.items.any { it.state == ItemState.FAILED }) {
                                    OutlinedButton(onClick = vm::retryFailed, enabled = !state.downloading) {
                                        Text("重试失败项")
                                    }
                                }
                                state.lastSavedUri?.let { uri ->
                                    TextButton(onClick = { openImage(uri) }) { Text("打开刚保存的图片") }
                                }
                                state.items.forEachIndexed { index, item ->
                                    Text("${index + 1}. ${when (item.state) {
                                        ItemState.PENDING -> "等待中"
                                        ItemState.DOWNLOADING -> "下载中"
                                        ItemState.SAVED -> "已保存"
                                        ItemState.SKIPPED -> "已跳过"
                                        ItemState.FAILED -> "失败"
                                    }} ${if (item.state == ItemState.FAILED) item.error else ""}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            HorizontalDivider()
                            OutlinedButton(onClick = vm::previewDemo, enabled = !state.downloading) {
                                Text("试用 3 张演示图片（非真实平台）")
                            }
                            Text("当前：X 为实验性公开解析；小红书、抖音、Threads 暂不支持完整解析。",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        1 -> Column(Modifier.padding(padding).padding(18.dp)) {
                            Text("最近下载记录", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(14.dp))
                            if (state.history.isEmpty()) Text("暂无下载记录")
                            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.history) { key ->
                                    ListItem(headlineContent = {
                                        Text(key, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    })
                                }
                            }
                        }
                        else -> Column(
                            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Text("偏好设置", style = MaterialTheme.typography.titleLarge)
                            ToggleSetting("分享后自动下载", preferences.autoDownload) {
                                vm.updatePrefs(preferences.copy(autoDownload = it))
                            }
                            ToggleSetting("跳过已保存图片", preferences.skipExisting) {
                                vm.updatePrefs(preferences.copy(skipExisting = it))
                            }
                            Text("同时下载数：${preferences.concurrency}")
                            Slider(
                                value = preferences.concurrency.toFloat(),
                                onValueChange = {
                                    vm.updatePrefs(preferences.copy(concurrency = it.toInt().coerceIn(1, 3)))
                                }, valueRange = 1f..3f, steps = 1
                            )
                            Text("相册保存目录（公共 Pictures 子文件夹）")
                            listOf("Pictures/OneTap Images", "Pictures/Paperize").forEach { value ->
                                Row {
                                    RadioButton(selected = preferences.folder == value,
                                        onClick = { vm.updatePrefs(preferences.copy(folder = value)) })
                                    Text(value, modifier = Modifier.padding(top = 13.dp))
                                }
                            }
                            Text("Paperize 是否索引该目录取决于其图库设置。无需相册读权限；卸载后图片保留。",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receive(intent)
    }

    @Suppress("DEPRECATION")
    private fun receive(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (uri != null && intent.type?.startsWith("image/") == true)
                    vm.shareUris(listOf(uri))
                else intent.getStringExtra(Intent.EXTRA_TEXT)?.let(vm::shareText)
            }
            Intent.ACTION_SEND_MULTIPLE -> if (intent.type?.startsWith("image/") == true) {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: arrayListOf()
                vm.shareUris(uris)
            }
        }
    }

    private fun openImage(uri: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(uri), "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (_: Exception) { /* No image viewer available. */ }
    }
}

@Composable
private fun ToggleSetting(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.padding(top = 12.dp))
        Switch(checked = value, onCheckedChange = onChange)
    }
}
