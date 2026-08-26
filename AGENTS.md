# AGENTS.md

## 正式版发布信息

本项目当前按“不上架应用商店”的方式发布正式版，分发物是侧载安装的 release APK。

### 应用信息

- 应用包名：`com.lipengzhou.mocklocation`
- 当前正式版：`versionName=1.6`，`versionCode=7`
- 最低系统版本：`minSdk=31`，即 Android 12+
- 构建产物路径：`build/release/MockLocation-1.6-release.apk`

### 开发版与正式版并存

Debug 构建固定使用独立包名 `com.lipengzhou.mocklocation.debug`，桌面显示名为 `模拟位置-debug`。Release 构建继续使用正式包名 `com.lipengzhou.mocklocation`，桌面显示名为 `模拟位置`。因此真机可以同时安装开发版和正式版，日常 Android Studio 运行或 `./gradlew :app:assembleDebug` 都会生成开发调试版，不会覆盖正式版。

Android 系统同一时间只能选择一个“模拟位置信息应用”。如果要切换开发版和正式版测试模拟定位，需要在开发者选项里重新选择对应应用。

### Release 签名

Release 签名所需的 keystore、签名配置、build-tools 版本和输出文件名都通过 `local.properties` 配置。高德正式版 Key 使用 `AMAP_API_KEY`，开发版如需独立高德 Key 可配置 `AMAP_API_KEY_DEBUG`；不配置时 debug 会回退使用 `AMAP_API_KEY`。示例配置如下：

```properties
sdk.dir=/path/to/Android/sdk
AMAP_API_KEY=your_amap_android_key
AMAP_API_KEY_DEBUG=your_amap_android_debug_key

# Release APK signing config. Keep real files and passwords out of git.
release.keystoreFile=/path/to/mocklocation-release.jks
release.signingConfigFile=/path/to/keystore.properties
release.buildToolsVersion=36.0.0
release.outputDir=build/release
release.apkName=MockLocation-1.6-release.apk
```

不要把 keystore、签名口令、真实 `local.properties` 或签名配置文件提交到 git。后续任何正式版升级都必须继续使用同一个 release keystore，否则 Android 会因为签名不一致拒绝覆盖安装，用户只能卸载重装。

### 发布构建流程

发布脚本位于 `scripts/release-apk.sh`。脚本会读取 `local.properties`，执行 release 构建、zipalign、签名、签名校验、对齐校验，并输出最终 APK 的 SHA256。

```bash
./scripts/release-apk.sh
```

### 设备安装验证

如果设备上已经安装过 debug 签名或其它签名的同包名版本，直接覆盖安装会失败并出现：

```text
INSTALL_FAILED_UPDATE_INCOMPATIBLE
```

需要先卸载旧版再安装正式版：

```bash
adb uninstall com.lipengzhou.mocklocation
adb install build/release/MockLocation-1.6-release.apk
```

卸载会清掉该应用本地数据。安装完成后可用以下命令确认设备端版本：

```bash
adb shell dumpsys package com.lipengzhou.mocklocation
```

### 用户侧使用前置条件

APK 通过非应用商店分发时，用户需要允许“安装未知来源应用”。安装后，用户仍需在 Android 开发者选项中把本应用设置为“模拟位置信息应用”，否则模拟定位功能无法正常注入位置。
