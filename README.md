# 安卓手机模拟定位

一个用于 Android 设备的模拟定位工具。应用支持地图选点、地点搜索、手动输入经纬度，并通过 Android 系统的“模拟位置信息应用”能力持续注入指定位置。

请仅在开发、测试或其它合法授权场景中使用。

## 功能

- 地图选点和当前位置定位
- 地点关键词搜索与搜索历史
- 手动输入经纬度，支持常见坐标系转换
- 启动、停止模拟定位，并可配置更新间隔
- 首次使用权限引导和运行状态诊断
- 应用内检查新版本并下载安装包

## 使用前置条件

- Android 12 及以上系统
- 已开启 Android 开发者选项
- 在开发者选项中将本应用设置为“模拟位置信息应用”
- 授权定位、通知等运行时权限

如果系统没有把本应用设置为模拟定位应用，应用可以打开并选点，但无法向系统注入模拟位置。

## 安装正式版

当前正式版：

- 包名：`com.lipengzhou.mocklocation`
- 版本：`1.7`（`versionCode=8`）
- APK：`MockLocation-1.7-release.apk`

可从 GitHub Release 下载 APK 后侧载安装。通过非应用商店安装时，需要允许“安装未知来源应用”。

也可以使用 adb 安装：

```bash
adb install build/release/MockLocation-1.7-release.apk
```

如果设备上曾安装过不同签名的同包名版本，覆盖安装可能失败。此时需要先卸载旧版：

```bash
adb uninstall com.lipengzhou.mocklocation
adb install build/release/MockLocation-1.7-release.apk
```

卸载会清除该应用的本地数据。

## 开发构建

项目使用 Kotlin、Jetpack Compose 和 Gradle 构建。开发前需要准备 Android SDK，并在 `local.properties` 中配置高德地图 Key：

```properties
sdk.dir=/path/to/Android/sdk
AMAP_API_KEY=your_amap_android_key
```

构建 debug 版本：

```bash
./gradlew :app:assembleDebug
```

Debug 包使用独立包名 `com.lipengzhou.mocklocation.debug`，桌面显示名为 `模拟位置-debug`，可以和正式版同时安装。Android 系统同一时间只能选择一个模拟定位应用，切换测试时需要在开发者选项中重新选择。

## 发布构建

正式版发布脚本：

```bash
./scripts/release-apk.sh
```

脚本会读取 `local.properties` 中的 release 签名配置，完成 release 构建、zipalign、签名、签名校验和 SHA256 输出。

不要提交真实的 `local.properties`、keystore 或签名口令。后续正式版升级必须继续使用同一个 release keystore，否则 Android 会拒绝覆盖安装。

更新检查清单位于 `release/update.json`，分发细节见 [docs/更新分发说明.md](docs/更新分发说明.md)。
