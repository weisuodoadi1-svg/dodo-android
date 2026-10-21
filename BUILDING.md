# 从修改源码构建

## 环境

- Python 3.9+；JDK 11。
- Android SDK Platform 35、Build Tools 35.0.0。
- `ANDROID_HOME` 指向 SDK；`JAVA_HOME` 指向 JDK。
- 原版中富通 1.3.0 APK，SHA-256 必须与 `version.json` 的 `base_sha256` 一致。
- 自己的本地 Android 签名 keystore。签名私钥和密码不应提交 Git。

构建工具的地址与 SHA-256 固定在 `tools.lock.json`。首次运行会下载到忽略目录 `.tools/`；每次使用前都会校验。解包及编译产物位于 `build/`，发布包位于 `dist/`，均不进入仓库。

## 构建 APK

将 keystore 密码写入环境变量 `DODO_STORE_PASSWORD`（建议交互输入，避免写进 shell 历史）。然后执行：

```sh
python3 scripts/build.py \
  --base-apk /path/to/original.apk \
  --keystore /path/to/release.keystore \
  --alias your-key-alias
```

生成 `dist/dodo-1.3.0-dodo.2.apk` 与 `dist/SHA256SUMS`。

构建过程会校验原包摘要、解包、应用资源改动、编译新增 Java 类、隔离 WebSocket 依赖、校验固定的接入点、对齐并签名。遇到原版结构不匹配时中止，不尝试猜测兼容。

原业务载荷保持原样。程序会从原包提取公开签名块，供原组件解码载荷；它不是私钥，也不能替他人签名。各用户登录与圈子权限检查仍在原服务中完成。

自行使用不同密钥构建的 APK 无法直接覆盖 Releases 中的安装包。请妥善保留自己的密钥，以便后续覆盖更新。

## Android 测试

测试 APK 必须与被测应用使用同一个密钥签名。在专用模拟器运行，避免干扰个人设备：

```sh
python3 scripts/build_tests.py --keystore /path/to/release.keystore --alias your-key-alias
adb -s YOUR_EMULATOR install -r --no-incremental dist/dodo-1.3.0-dodo.2.apk
adb -s YOUR_EMULATOR install -r --no-incremental build/android-tests/tests.apk
adb -s YOUR_EMULATOR shell am instrument -w local.zfttests/.Smoke
adb -s YOUR_EMULATOR shell am instrument -w local.zfttests/.Navigation
adb -s YOUR_EMULATOR shell am instrument -w local.zfttests/.Splash
adb -s YOUR_EMULATOR shell am instrument -w local.zfttests/.Article
```

还可运行 `.ChatMedia`、`.ChatActions`、`.History`、`.Stocks`、`.AiNews`。资讯与行情测试依赖公开接口可用性；测试不自动发送真实聊天消息。示例消息位于测试模式，非内置聊天记录。

## 目录

- `src/local/zft/`：原生聊天、股票容器、AI 阅读及导航。
- `assets/stocks/`：自选、行情数据和图表。
- `assets/local-zft-emoji/`：表情资源。
- `res/`、`resource-patches.json`：布局、配色、应用名和开屏图改动。
- `scripts/`：构建与依赖隔离。
- `tests/`：模拟数据与回归检查。
