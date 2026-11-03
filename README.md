# 打招呼助手

一个安卓小工具：在任何招聘 App 的岗位详情页，点一下屏幕上的悬浮球，用你自己的简历和 JD 生成一段开场白，自动复制到剪贴板，回聊天框粘贴即可发送。

不依赖任何招聘平台的后台接口，不模拟点击，不自动发送。它只是帮你把「看 JD、想措辞、打字」这几步省掉。

## 特性

- **一行都不用截图**：通过无障碍服务直接读取当前屏幕的文本节点，不截屏、不做 OCR，速度快、准确率高，也不受 App 的防截屏影响。
- **话术由大模型生成**：任何 OpenAI 兼容接口都能用（DeepSeek、通义、Kimi、自建中转均可），提示词里强制要求只使用简历中真实存在的信息。
- **简历自己配**：直接粘贴，或从 `.txt` / `.docx` 导入（docx 走 zip + XML 解析，不引第三方库）。
- **字数自己定**：50 / 100 / 200 快捷选择，也可填任意值。是个大概目标，模型会按内容自然伸缩。
- **没配 API 也能用**：内置一套按岗位类型匹配的模板引擎兜底，配了 Key 之后自动切换成大模型生成。
- **零第三方依赖**：不使用 AndroidX、不使用 Gradle，整个 APK 不到 40 KB。
- **结果可改可复制**：生成后自动复制，也可以点「填入输入框」直接写进已聚焦的输入框。

## 权限说明

只需要两个权限，都不涉及账号或隐私数据：

| 权限 | 用途 |
| --- | --- |
| 无障碍服务 | 读取当前屏幕上的文字。**只在点击悬浮球的那一刻读一次**，不监听、不记录、不保存。 |
| 网络（INTERNET） | 把读到的岗位文字和你的简历发给**你自己配置的**模型接口。 |

悬浮球使用 `TYPE_ACCESSIBILITY_OVERLAY` 绘制，因此**不需要**「显示在其他应用上层」权限。

APK 不申请存储、通讯录、位置、电话等任何权限，也没有埋点和统计。

## 安装

从 Releases 下载 APK 直接安装。首次开启无障碍时，如果系统提示「受限设置」：

> 设置 → 应用 → 打招呼助手 → 右上角三点 → 允许受限设置 → 回到无障碍页面重新开启

这是 Android 13 之后对非商店安装应用的限制，属于正常现象。

## 配置

打开 App，依次填：

1. **接口地址**：如 `https://api.deepseek.com/v1/chat/completions`
2. **模型名**：如 `deepseek-chat`
3. **API Key**
4. **期望字数**：默认 100
5. **我的简历**：粘贴文本，或从文件导入

点「测试接口连通性」确认能通，再点「保存设置」。

配置全部存在本机 `SharedPreferences`，不会上传到任何地方。

## 使用

1. 打开任意招聘 App，进到岗位详情页
2. 点蓝色悬浮球「话」
3. 等 2–5 秒，弹出开场白（已自动复制）
4. 回聊天框长按粘贴，看一眼再发送

悬浮球可以拖动，松手后停在原地。

## 自己编译

项目不使用 Gradle，用 SDK 命令行工具直接构建。需要 JDK 11+ 和 Android SDK（build-tools 35、platform android-34）。

```bash
# 1. 编译资源
aapt2 compile --dir app/res -o build/res.zip

# 2. 链接资源与清单（会同时生成 R.java）
aapt2 link -o build/base.apk -I $ANDROID_HOME/platforms/android-34/android.jar \
  --manifest app/AndroidManifest.xml -R build/res.zip --java build/gen \
  --min-sdk-version 26 --target-sdk-version 34

# 3. 编译 Java
javac --release 8 -encoding UTF-8 -classpath $ANDROID_HOME/platforms/android-34/android.jar \
  -d build/classes @sources.txt

# 4. 转 dex
d8 --min-api 26 --lib $ANDROID_HOME/platforms/android-34/android.jar \
  --output build/dex @classes.txt

# 5. 把 classes.dex 塞进 APK，对齐，签名
zipalign -f -p 4 build/unsigned.apk build/aligned.apk
apksigner sign --ks debug.keystore --out 打招呼助手.apk build/aligned.apk
```

完整脚本见 `build.sh`。也可以直接用 Android Studio 打开 `app/` 目录（需要自己补一个 Gradle 配置）。

> 已知坑：build-tools 34 自带的 d8 处理匿名内部类会抛 `NullPointerException`，用 35 或更高版本。

## 已知限制

- **PDF 简历不支持**，请用 WPS 打开复制文本，或另存为 `.docx`
- 部分 App 的页面是自绘视图，节点树里没有文字，这种情况下读不到内容（会提示「没读到内容」）
- 生成依赖网络，弱网下可能要等更久
- 只适配竖屏手机，没有做过平板和横屏

## 它不做什么

- 不自动点击「打招呼」
- 不自动发送
- 不批量群发
- 不上传你的简历或聊天记录到作者服务器（作者没有服务器）

最后一步永远留给你自己按下。

## License

MIT
