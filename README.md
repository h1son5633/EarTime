# EarTime · 耳机佩戴统计

记录蓝牙耳机佩戴时长的安卓 App：从安装当天开始，只要蓝牙耳机（A2DP / 耳机协议）保持连接，就自动记一段佩戴记录，并按「今天 / 统计 / 设备」三页展示累计时长、个人纪录和分设备统计。

- 包名：`com.h1son5633.eartime`
- Kotlin + Jetpack Compose，minSdk 26，targetSdk 35
- 会话存本地 Room 数据库，不联网
- CI 构建 unsigned release APK，正式包在本地用固定密钥签名后发 Release
