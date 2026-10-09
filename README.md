# OneTap Images（一键存图）

免费、开源、无需 Root 的 Android 静态图片批量保存工具。Kotlin / Jetpack Compose / MediaStore，支持中文与深色模式。

> 开发中 (0.1.0-alpha)。**尚未经过手机实测，不保证平台解析成功。**

## 已编写的 MVP 功能
- Android 系统分享菜单接收文字链接，以及单张/多张图片 URI。
- 从分享文案识别小红书、抖音、X/Twitter、Threads 的 URL。
- 通过内置示例测试多张公开 JPEG 图片的并发下载与相册保存。
- 下载到公共 `Pictures/OneTap Images` 或 `Pictures/Paperize`；MediaStore 的 IS_PENDING 与损坏文件删除。
- 默认两路并发、进度与失败重试、任务取消和基础跳过重复下载。
- X/Twitter：公开嵌入 JSON 的**实验性**解析，只处理当前推文顶层的 photo，未对真实公开帖子验证。
- 小红书、抖音、Threads：**目前仅识别链接，不支持解析图片列表。** 不会以封面冒充成功。

## 安装和构建
仓库的 GitHub Actions 在推送分支和 PR 时运行 Android 单元测试、lint 和 Debug APK 构建。
访问 **Actions → Android CI → 最近一次运行 → Artifacts → OneTap-Images-debug-apk**，下载解压 APK 后在一加 Ace 2 打开安装。
仓库使用 Gradle 8.13、JDK 17、Android SDK 35。本地需安装 Android Studio 或相应 Android SDK/Gradle。

使用：原生分享菜单选择「一键存图」，或复制链接到应用粘贴；如果没有选择「分享后自动下载」，先预览再点「全部下载」。

## 已知限制
- 四个平台真实图片提取未经过完整验收，实验性 X 数据入口可能不稳定或受平台规则限制。
- 下载状态未持久化为后台 WorkManager 任务；进程终止后需要重新发起。
- 相册保存目录当前是两个固定公共目录，并非任意自定义 SAF 文件夹。
- 删除应用不会删除已保存图片，但会删除下载历史，所以重装后去重记录消失。
- Pictures/Paperize 需要 Paperize 自己识别/配置；并未集成它。

实现边界、参考项目许可证与测试清单参见 docs/。本仓库原创代码采用 MIT；没有移植第三方 GPL 项目的代码。
