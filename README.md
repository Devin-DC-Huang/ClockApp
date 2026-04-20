# ClockApp - Android 闹钟应用

一款功能丰富的现代化 Android 闹钟应用，采用 Material Design 3 设计风格。

## 功能特性

### 闹钟类型
- **指定日期闹钟** - 为特定日期设置闹钟，支持多选日期
- **特殊闹钟** - 自动同步中国工作日历，支持三种响铃模式：
  - 所有工作日（周一至周五，自动跳过节假日）
  - 仅假期后第一个工作日（包括周末后的周一）
  - 所有节假日（包括法定节假日和周末）
- **普通闹钟** - 按周重复的闹钟，可自定义重复日期

### 核心功能
- 全屏响铃，黑色背景，时间居中显示
- 后台闹钟支持（即使应用被杀死也能正常响铃）
- 贪睡功能，每个闹钟可独立设置贪睡时长（默认5分钟）
- 振动支持
- 闹钟复制功能
- 针对国产手机厂商的电池优化引导（华为、小米、OPPO、vivo 等）
- 12月跨年提醒，提示用户设置下一年闹钟

### 数据预取与跨年支持
- 支持预取下一年工作日历数据（12月开放，通过左下角菜单进入）
- 预取时自动为特殊闹钟追加新一年日期，无需手动重新设置
- 自动缓存节假日信息，离线可用

### 可靠性
- 使用精确闹钟调度，确保准时响铃
- 前台服务确保闹钟可靠送达
- 开机广播接收器，设备重启后自动重新调度闹钟
- 单闹钟实例 - 同一时间只响一个闹钟（行业标准）

## 技术栈

- **语言**: Kotlin
- **UI**: Material Design 3 (Material You)
- **架构**: MVVM，使用 ViewModel 和 LiveData
- **数据库**: Room 持久化库
- **网络**: Retrofit + Gson（用于节假日数据）
- **异步**: Kotlin 协程
- **最低 SDK**: 29 (Android 10)
- **目标 SDK**: 36 (Android 16)

## 权限说明

应用需要以下权限：

- `SCHEDULE_EXACT_ALARM` - 用于精确的闹钟定时
- `USE_EXACT_ALARM` - 用于精确闹钟调度（Android 12+）
- `WAKE_LOCK` - 用于唤醒设备响铃
- `FOREGROUND_SERVICE` - 用于可靠的前台闹钟服务
- `POST_NOTIFICATIONS` - 用于闹钟通知
- `RECEIVE_BOOT_COMPLETED` - 用于重启后重新调度闹钟
- `VIBRATE` - 用于闹钟振动
- `SYSTEM_ALERT_WINDOW` - 用于在锁屏上显示闹钟

## 构建指南

### 环境要求
- Android Studio Ladybug 或更新版本
- JDK 11 或更高版本
- Android SDK API 36

### 构建步骤

1. 克隆仓库：
```bash
git clone https://github.com/yourusername/ClockApp.git
cd ClockApp
```

2. 在 Android Studio 中打开并同步项目

3. 构建 APK：
```bash
./gradlew assembleDebug
```

或构建发布版：
```bash
./gradlew assembleRelease
```

## 安装说明

### 从源码安装
1. 按照上述步骤构建 APK
2. 将 APK 传输到设备
3. 安装并授予必要权限

### 必要设置
为确保闹钟在国产手机上正常工作，请进行以下设置：

1. **关闭电池优化**（针对本应用）
2. **允许自启动**
3. **允许后台活动**

不同品牌的设置路径：
- **华为/荣耀**: 设置 > 电池 > 耗电管理 > 启动管理
- **小米/红米**: 设置 > 省电与电池 > 应用智能省电
- **OPPO/一加**: 设置 > 电池 > 应用耗电管理
- **vivo/iQOO**: 设置 > 电池 > 后台耗电管理

## 项目架构

```
app/
├── data/
│   ├── api/          # Retrofit API（节假日数据）
│   ├── db/           # Room 数据库和 DAO
│   ├── model/        # 数据模型（Alarm、CalendarData）
│   └── repository/   # 数据仓库
├── receiver/         # 广播接收器（闹钟、开机）
├── service/          # 闹钟前台服务
└── ui/
    ├── alarm/        # 闹钟详情/编辑页面
    ├── main/         # 闹钟列表主页面
    └── ring/         # 闹钟响铃页面
```

## 节假日数据来源

应用使用 [holiday-cn](https://github.com/NateScarlet/holiday-cn) 提供的中国节假日数据，通过 jsDelivr CDN 获取，以准确判断工作日和节假日。

## 开源协议

本项目采用 MIT 协议开源 - 详见 [LICENSE](LICENSE) 文件。

## 技术支持

如遇到任何问题或有疑问，请 [提交 Issue](https://github.com/yourusername/ClockApp/issues)。

---

**注意**: 本应用主要面向中国用户设计，支持中国工作日历。部分功能可能需要针对其他地区进行适配。
