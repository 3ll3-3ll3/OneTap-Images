# 测试矩阵

| 项目 | 状态 |
| --- | --- |
| URL 平台识别、X 状态 ID、文件名安全函数 | 编写 JUnit，等待 CI |
| X 单帖顶层 photo 过滤、视频封面排除、ID 不匹配 | 编写 JUnit，等待 CI |
| Gradle `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` | 等待 CI |
| 系统分享 text/plain、单图、多图 | 未在真实 ColorOS 16 测试 |
| 本地系统相册保存、卸载后保留 | 未在真机测试 |
| 三张合法公开示例图下载 | 下载器代码完成、未完成实测 |
| X 真实单图/多图/视频/引用帖 | 未经真实数据测试 |
| 小红书、抖音、Threads 真实帖子 | 解析器尚未实现 |
| Paperize 文件夹索引 | 需 Paperize 端验证 |

不得将单元测试/示例图片下载视为四个平台实际支持的证据。
