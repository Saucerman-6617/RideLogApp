# 🚲 RideLogApp · 骑行打卡

> 一款只做"记录 + 统计"的轻量骑行打卡 Android App。手动录入距离与骑行路线，本地存储，可选坚果云 WebDAV 备份。

## 设计原则

- **单核心闭环**：打开 → 打卡 → 看到统计，3 步内完成
- **无账号 / 无社交 / 无定位追踪**：不收集任何个人数据
- **全离线可用**：网络仅在备份/恢复时使用
- **V1 不做**：GPS 轨迹、地图、配速、多设备实时同步、后台保活

## ✨ 功能

### 🏠 主页
- 月历视图（7×6 网格，横滑 / 箭头切月）
- 日历标记：骑行次数（如"3次"） 或 当日里程数字（可切换）
- 天气卡片：当前位置 + 天气预报（Open-Meteo 免费接口，本地缓存）
- 当日汇总条 + 全宽记录按钮
- BottomSheet 录入：日期 / 时间 / 距离（拨轮选择）/ 备注 / 骑行路线
- **骑行路线**：起点 + 终点双输入框，未填写时记录中保持空白
- **常用地址**：路线输入框下方快捷标签，点击自动填入当前输入框，长按编辑，末尾「+」新增；支持家 / 公司 / 学校 / 健身 / 公园等 10 种图标
- 记录列表按 1:2:1 展示「时间 + 备注 / 骑行路线 / 公里数」，路线自动换行、超过两行省略
- 距离校验（0.1 – 9999.9 km）+ 备注 200 字上限
- Snackbar + 5 秒撤销（新增 / 删除均可）

### 📊 统计页
- 日 / 周 / 月 分段切换
- 4 项汇总卡片（次数、总公里、日均、单日最高）
- **Compose Canvas 自绘图表**（无第三方库）：
  - 日视图：24 小时时间轴散点
  - 周视图：7 根柱 + 选中数据气泡
  - 月视图：日历热力图 + 颜色图例
- 附加指标：连续打卡天数（streak）+ 本月 vs 上月环比

### ⚙️ 设置
- 主题（跟随系统 / 浅色 / 深色）
- 日历标记样式切换（骑行次数 / 里程数字）
- JSON + CSV 本地导出（含骑行路线字段）
- 本地 JSON 导入（合并 / 覆盖 冲突策略）
- 清空全部数据（输入"删除"二次确认）

### ☁️ 可选 · 坚果云 WebDAV
- 服务器地址 / 账号 / 应用密码配置
- HTTPS 加密存储（EncryptedSharedPreferences）
- 全量备份（骑行记录 + 常用地址）+ 远端列表浏览 + 一键恢复
- 自动备份开关：记录或常用地址变更后自动上传
- 恢复前自动生成快照，失败可回滚
- 合并 / 覆盖 两种冲突策略，常用地址同步恢复（合并按 id 去重）

## 🛠 技术栈

| 层 | 选型 |
|---|---|
| 语言 | Kotlin 2.2 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository |
| 存储 | Room (SQLite) + DataStore Preferences |
| 加密 | EncryptedSharedPreferences |
| 网络 | OkHttp + kotlinx.serialization |
| 图表 | Compose Canvas 自绘 |
| minSdk / targetSdk | 26 / 36 |
| 构建 | Gradle Kotlin DSL + version catalog |

## 📦 目录结构

```
com.example.cyclingcheckin/
├── navigation/          # NavHost + 路由
├── ui/
│   ├── theme/          # Color / Type / Theme
│   ├── home/           # 主页 + BottomSheet
│   ├── stats/          # 统计页 + Canvas 图表
│   ├── settings/       # 设置 + WebDAV 配置
│   └── components/     # 共享组件
├── data/
│   ├── local/          # Room Entity + DAO
│   ├── prefs/          # DataStore + 加密存储
│   ├── repo/           # Repository 实现
│   └── remote/         # WebDAV Client + BackupService
├── domain/model/       # Record / Stats / Validators
└── util/               # TimeUtils / FileUtils
```

## 📥 下载

前往 [Releases](https://github.com/Saucerman-6617/RideLogApp/releases) 下载最新 APK（当前版本 **v1.1.1**），Android 8.0+ 可直接安装。

## 🚀 快速开始

### 环境要求
- Android Studio Koala 或更新版本
- JBR 25+（Android Studio 自带）
- Android SDK 36.1 + Build Tools 36.1

### 构建

```bash
# 克隆
git clone https://github.com/Saucerman-6617/RideLogApp.git
cd RideLogApp

# 调试构建（首编译约 5 分钟，二次约 2 分钟）
./gradlew :app:assembleDebug

# APK 输出路径
# app/build/outputs/apk/debug/app-debug.apk
```

### 安装到模拟器 / 真机

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 🔐 隐私声明

- 所有骑行数据**仅存本机**（Room SQLite 数据库 + DataStore）
- WebDAV 备份默认手动触发，可在设置中开启自动备份，不配置则不产生任何上传
- 天气功能使用 Open-Meteo 免费接口，仅请求大致位置对应的天气，不上传骑行数据
- 不申请任何危险权限（仅需 INTERNET 用于天气与备份）
- 提供"清空全部数据"一键删除所有记录

## 📝 开源许可

本项目基于 **Apache License 2.0** 开源 — 详见 [LICENSE](LICENSE) 文件。

所有主要依赖（AndroidX、Jetpack Compose、Room、OkHttp、kotlinx.serialization 等）均为 Apache 2.0。
