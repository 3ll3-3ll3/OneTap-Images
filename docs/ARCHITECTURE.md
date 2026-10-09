# OneTap Images 0.1.0 架构

```text
Android Intent.ACTION_SEND(text/plain/image/*) / SEND_MULTIPLE(image/*)
  -> MainActivity -> MainViewModel
  -> LinkTools (URL 和平台识别) -> ExtractorRegistry
      -> XExtractor (非正式公开嵌入 JSON，实验性)
      -> UnsupportedExtractor (小红书/抖音/Threads，明确失败)
      -> demo image fixtures
  -> ImageDownloadManager (2~3 并发、限量缓存、重试/取消)
  -> MediaStoreSaver (格式+尺寸校验、IS_PENDING、失败清理)
  -> AppSettings (DataStore 配置和下载历史)
```

- 提取器与下载器隔离，`Extraction.Success` 中只包含**当前帖子**的静态图片。
- X 实验解析只处理根级 `mediaDetails` 的 `photo` 和 pbs.twimg.com/media 图片。
- 不抓 Cookie、不接收密码、不跨越验证码、非公开内容、付费墙或平台访问控制。
- Android 10+ 写入自身新建的公共 MediaStore 图片不需要存储权限；卸载后图片保留。
- 网络 HTTP 请求只接受 HTTPS 图片，拒绝视频和 HTML 假图片，单图大小上限 50 MiB。
- 缓存先下载完整再检查解码，再 MediaStore 发布，错误会删除 Pending 记录。
- 进程停止后的自动恢复、任意目录 SAF、后台通知和更可靠的去重是后续工作。
- Paperize 仅共用 Pictures/Paperize 文件夹，Paperize 的索引能力需另行验证。

## 参考项目与许可证
- [XHS-Downloader](https://github.com/JoeanAmier/XHS-Downloader) GPL-3.0
- [TikTokDownloader](https://github.com/JoeanAmier/TikTokDownloader) GPL-3.0
- [gallery-dl](https://github.com/mikf/gallery-dl) GPL-2.0

这些项目仅作为架构和元数据格式研究参考；本仓库未复制受 GPL 保护的源代码。
