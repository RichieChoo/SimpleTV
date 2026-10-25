# 换电脑、换电视时复用

本目录保存 Apple桌面的基础布局和 IPTV 的公开频道源配置。桌面源码及五段 1080p 航拍素材也在本仓库；APK 已内置布局、月夜背景和航拍，无须重新制作。

## 换电脑

同一台电视已经安装的应用和配置保存在电视上，换电脑不需要重装。需要维护代码时，在新电脑克隆本仓库即可。

使用 JDK 17、Android SDK 34 构建：

```sh
./launcher/gradlew -p launcher --no-daemon \
  -PlauncherApplicationId=com.ts.launcher \
  testReleaseUnitTest lintRelease assembleRelease
```

输出文件为 `launcher/app/build/outputs/apk/release/app-release.apk`。本仓库不存放预编译 APK 或签名密钥。给已有电视覆盖升级时须沿用原签名密钥；密钥应私下迁移，不能提交到仓库。

## 换电视

1. 安装 Apple桌面 APK。布局和内置背景随 APK 安装，未安装的首页应用会从电视现有应用中补位；「全部应用」保留在末尾。
2. 在电视支持的默认桌面选择页中选择 Apple桌面。应用声明了 HOME，开机启动默认开启；厂商对第三方桌面的限制需要逐台确认。原厂桌面仍保留，可从「更多 → 恢复原桌面」返回。
3. 安装 [StreamVault 1.2.2](https://github.com/Davidona/StreamVault-IPTV/releases/tag/v1.2.2)，将 [streamvault-public-sources.json](iptv/streamvault-public-sources.json) 复制到电视。
4. 在 **StreamVault 自己的设置 → 备份与恢复 → 设备与 USB 备份 → 导入数据** 中选择此 JSON。可用电视文件管理器以 StreamVault 打开该文件。导入提供商和偏好设置，冲突策略选择「保留现有」。
5. 在提供商页面同步频道，选择「IPTV 中文公开频道」。默认合并同频道的不同线路，并设为稳定性优先。直播页左上角的来源菜单可切换中文与全球；中间为空时，选「All Channels」，空的 Favorites 只代表尚未收藏频道。

备份保存两个公开 M3U 地址、中文优先的选择和直播页来源菜单，不包含电视 IP、Wi-Fi、账号密码、观看记录或 ADB 密钥。频道列表由新电视联网同步，数量及可播放状态会随公开源变化。

本次实机已有中文 219 条原始线路、全球 11,320 条。2026-10-25 电脑直连抽取清单和首分片，中文 219 条中 80 条通过该次检查；该数字不等于电视实播通过数。电视播放器已记录 CCTV-1、CCTV-13 的 720p 线路成功出帧；CCTV-6 的当前标清线报 Source error，高清线拒绝连接，尚未修复。StreamVault 1.2.2 的自定义分组备份解析存在兼容问题，因此此备份采用独立中文提供商，不包含未能导入的自定义分组。

## 桌面基础配置

[launcher/config.json](launcher/config.json) 是 APK 内置布局的可读副本。它包含单行导航、应用顺序、图标标题和背景参数；不包含某台电视上的应用登录或系统默认桌面授权。新增修改应同时更新 APK 的 `launcher/app/src/main/assets/simpletv-default.json`。

原始用户数据备份留在本机 `work/`，不会上传到公开仓库。需要迁移自己收藏、观看历史或登录状态时，应单独导出并私下保存。
