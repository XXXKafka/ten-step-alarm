# 十步闹钟 (Ten-Step Alarm)

一个用 **Kotlin + Jetpack Compose + Material 3** 编写的 Android 闹钟应用（个人/侧载使用），核心卖点是**响铃后必须完成挑战才能关闭**，并附带番茄钟、计时器/秒表、翻页时钟等工具。

**当前版本**：1.1.1（versionCode 3）

## 功能总览

1. **闹钟**：多闹钟管理（时间 / 星期重复 / 标签 / 铃声 / 音量 / 震动 / 贪睡），`AlarmManager.setAlarmClock` + 前台服务保证准时响铃，**响铃后必须完成挑战才能关闭**。
2. **番茄钟**：默认 25 分钟专注 / 5 分钟休息，时长可自定义，结束时有通知和提示音。
3. **计时器 + 秒表**：计时器支持预设/自定义分钟（1–180）、退到后台到点仍有提示音通知；秒表支持启动/暂停/复位/计次。
4. **翻页时钟**：应用内全屏翻页时钟（普通数字显示，无翻页动画），屏幕常亮、沉浸式隐藏系统栏；点击全屏后整屏含状态栏显示翻页时钟背景色，不再显示应用主题色。
5. **主题与设置**：跟随系统深浅色并可手动覆盖（含 **AMOLED 纯黑**）；主题颜色**默认黑白灰（monochrome）**，并可选动态取色 / 预设色板 / 自定义颜色；中英文切换。

底部导航共 5 项：**闹钟 / 番茄钟 / 计时器 / 时钟 / 设置**。

## 技术栈与版本

| 组件 | 版本 |
|---|---|
| 语言 | Kotlin 2.4.10（AGP 9 内置 Kotlin，Compose 编译器插件 2.4.10） |
| Gradle | 9.5.0（Wrapper） |
| Android Gradle Plugin | 9.3.0 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 |
| UI | Jetpack Compose BOM 2026.06.00（Compose 1.11.x，Material3 1.4.0） |
| 持久化 | Room 2.8.0（KSP 2.3.10，schema 已导出）+ DataStore Preferences 1.1.7 |
| 导航 | Navigation Compose 2.9.0 |
| 相机扫码 | CameraX 1.6.1 + ML Kit Barcode Scanning 17.3.0 |
| 测试 | JUnit 4.13.2 + Turbine 1.2.1 + Robolectric 4.16.1 |

> 说明：AGP 9 默认启用“内置 Kotlin”，因此工程**没有**应用 `org.jetbrains.kotlin.android` 插件；Kotlin 编译器版本通过根 `build.gradle.kts` 的 `buildscript` 固定为 2.4.10，与 Compose 编译器插件、KSP 保持一致。

## 如何构建运行

### 方式一：Android Studio（推荐）

1. 安装 **Android Studio 2026.x**（AI-261 及以上，支持 AGP 9）与 JDK 17+（AS 自带 JBR 即可）。
2. `File → Open`，选择本项目根目录（`settings.gradle.kts` 所在目录），等待 Gradle Sync 完成。
3. 首次同步会自动下载依赖；若提示缺少 SDK 组件，按提示安装 **Android SDK Platform 37** 与 **Build Tools 36.0.0**。
4. 连接真机或启动模拟器，点击 Run（绿色三角）即可安装运行。

### 方式二：命令行（使用本机 Android Studio 环境）

```powershell
# 使用 Android Studio 自带的 JDK（JBR）与项目 wrapper：
cd D:\Users\Desktop\New\alarm
& "D:\Program Files\Android\Android Studio\jbr\bin\java.exe" -jar .\gradle\wrapper\gradle-wrapper.jar :app:assembleDebug
& "D:\Program Files\Android\Android Studio\jbr\bin\java.exe" -jar .\gradle\wrapper\gradle-wrapper.jar :app:testDebugUnitTest

# 或设置 JAVA_HOME 后直接：
.\gradlew.bat :app:assembleDebug
# APK 输出位置：app/build/outputs/apk/debug/十步闹钟-debug.apk
```

> 提示：`gradlew.bat` 对带空格的 JAVA_HOME 路径（如 `...\Android Studio\jbr`）校验较严格，若报 “invalid directory”，可直接用上面的 `java -jar gradle-wrapper.jar` 方式（等价于 AS 的构建环境）。

## 功能说明

### 闹钟

- **添加 / 编辑 / 删除**：主界面右下角 `+` 添加；点击卡片进入编辑页。可设置时间、周一至周日多选重复、标签、铃声（系统铃声选择器）、音量（0–100%）、是否震动、贪睡开关。
- **重复规则**：选了任意星期 → 每周按所选星期重复；**一个都不选 → 一次性闹钟**，响铃后自动停用。
- **苹果风格设定页**：编辑页采用 iOS 时钟风格——居中标题 + 取消/保存，**时分上下滚动转轮选择时间**，分组列表（重复 / 标签 / 铃声 / 音量 / 震动 / **挑战** / 稍后提醒 / 红色删除行）。
- **准时响铃**：`AlarmManager.setAlarmClock`（精确闹钟，状态栏显示闹钟图标）+ `AlarmRingingService` 前台服务（`specialUse`）播放铃声/震动/保持 CPU，并发出带 `fullScreenIntent` 的高优通知拉起全屏响铃页（响铃期间每 8 秒重投一次，防止被系统/OEM 关闭）；**铃声 5 秒渐强**避免惊扰；**内置兜底铃声**：settings/media 铃声 URI 解析或播放失败时自动退回 `res/raw/alarm_fallback.wav`，保证一定有声音。
- **勿扰模式也能响铃**：`USAGE_ALARM` + `setAlarmClock`，响铃通知渠道 `setBypassDnd`。
- **挑战关闭闹钟**：每个闹钟可在编辑页“挑战”分组选择关闭方式，未完成时“关闭闹钟”按钮禁用并显示提示：
  - **步数**（默认，目标 1–100）：优先系统计步传感器 `TYPE_STEP_COUNTER`（需 `ACTIVITY_RECOGNITION` 权限，首次启动请求）；缺失或被拒时**降级为加速度计估算**并提示；两者都不可用时按钮保持禁用并提示。
  - **数学题**：响铃页生成一道两位数加减法，答对即可关闭。
  - **摇晃手机**：加速度计检测摇晃（默认 20 次）。
  - **扫码（QR）**：CameraX 取景框扫到任意二维码即可关闭；相机权限被拒或不可用时**自动回退为步数挑战**。
- **稍后提醒 / 贪睡**：默认 5 分钟（设置页可改），可无限次。
- **开机重建**：`BootReceiver` 监听 `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`，重启或更新后自动重建所有启用中的闹钟。
- **精确闹钟权限恢复自动重排**：监听 `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`，用户授权后无需重开应用即可恢复调度。
- **小米/HyperOS 兼容**：设置页提供自启动、后台弹出界面、忽略电池优化引导；若开发者选项“系统优化”开启导致全屏通知被撤销（只剩锁屏通知），需关闭该选项并重装应用。

### 番茄钟

- 专注（默认 25 分钟）/ 休息（默认 5 分钟）倒计时，可开始 / 暂停 / 重置 / 手动切换模式；专注轮数累计显示。
- 时长在设置页用滑块调整（专注 5–90 分钟，休息 1–30 分钟）。
- 结束铃声可分别设置；结束时播放铃声、发通知并弹出结束提示页。

### 计时器 + 秒表

- 入口：底部「计时器」Tab，页内用标签切换**计时**与**秒表**。
- **计时**：预设 1/3/5/10/25 分钟 + 滑块微调与自定义输入（1–180 分钟）；开始 / 暂停 / 重置；退到后台甚至被系统清理后，到点仍会发**带提示音的通知**（`setExactAndAllowWhileIdle` + 声音通知）。
- **秒表**：开始 / 暂停 / 复位 / 计次，显示 分:秒.百分秒。

### 翻页时钟

- 入口：底部「时钟」Tab，**翻页时钟样式**：每个数字为普通数字显示（无翻页动画），分钟/秒变化直接刷新；横竖屏自适应、不锁定方向。
- **屏幕常亮**（`FLAG_KEEP_SCREEN_ON`），进入即隐藏状态栏与导航栏（沉浸式）。
- **3 秒自动沉浸**：3 秒后除时间/日期外的控件自动淡出；**轻点屏幕唤出**。
- **时间背景色可调**（设置 → 翻页时钟 → 时钟配色）：跟随主题 / 浅色 / 深色 / 自定义，文字自动黑/白高对比；**点击全屏后背景铺满整屏（含状态栏区域），不再显示应用主题色**。

### 设置

- **界面语言**：跟随系统 / 中文 / English，切换立即生效。
- **主题模式**：跟随系统 / 浅色 / 深色 / **纯黑（AMOLED）**。
- **主题颜色**：**黑白灰（默认）** / 动态取色 / 预设色板 / 自定义颜色（HSL 取色器）。
- **时间格式**：24 小时制开关（影响闹钟编辑转轮与翻页时钟）。
- **闹钟守护（后台常驻）**：防止从最近任务关闭应用后闹钟无法响铃；**默认仅小米/红米开启，且只在“存在启用闹钟”时运行**，无闹钟自动停止。
- **番茄钟 / 贪睡 / 翻页时钟**：时长、铃声、配色等细项。
- **权限**：精确闹钟、通知权限状态与引导；默认铃声选择。

## 工程结构（要点）

```
app/src/main/java/com/tenstep/alarm/
├── MainActivity.kt                  # 入口：edge-to-edge、权限请求、启动时重排闹钟
├── TenStepApplication.kt            # Application（语言注入）+ AppContainer（手动依赖容器）+ 守护服务智能启停观察
├── alarm/                           # 闹钟核心
│   ├── NextTriggerCalculator.kt     # 下次触发时间纯计算
│   ├── AlarmScheduling.kt           # 调度接口 + AlarmManager 封装（setAlarmClock/贪睡/取消）
│   ├── AlarmReceiver.kt             # 到点广播 → 前台服务 + 全屏页面 + 重复/一次性处理
│   ├── BootReceiver.kt              # 开机/更新后重建闹钟
│   ├── ExactAlarmPermissionReceiver.kt # 精确闹钟权限恢复后自动重排
│   ├── AlarmRingingService.kt       # 响铃前台服务（铃声渐强/震动/唤醒/挑战传感器）
│   ├── MonitorController.kt         # 闹钟守护启停判定
│   ├── StepDetector.kt / StepGate.kt        # 步数算法（纯逻辑）与传感器门
│   ├── ShakeDetector.kt / ShakeGate.kt      # 摇晃算法（纯逻辑）与传感器门
│   ├── MathChallenge.kt / ChallengeEvaluator.kt # 数学题生成与挑战判定（纯逻辑）
│   └── RingingSession.kt            # 响铃状态共享（服务 ↔ UI）
├── data/                            # Room(AlarmEntity/DAO/DB，v3 含挑战字段) + DataStore(SettingsStore)
├── timer/                           # 计时器/秒表：引擎（纯逻辑）、ViewModel、TimerScreen、TimerReceiver
├── ui/
│   ├── theme/                       # M3 主题（动态取色/预设/自定义 + AMOLED + HSL 色板）
│   ├── navigation/AppNavigation.kt  # 底部导航（5 项）+ NavHost
│   ├── alarm/                       # 闹钟列表 / 编辑页（含挑战分组）
│   ├── ringing/                     # 全屏响铃页（步数/数学/摇晃/扫码挑战面板）
│   ├── pomodoro/                    # 番茄钟页
│   ├── timer/                       # 计时器/秒表页
│   ├── clock/                       # 翻页时钟页
│   └── settings/                    # 设置页（SettingsScreen + SettingsSections 分区）
└── util/                            # 通知渠道 + 应用内语言切换（LocaleHelper）
```

## 测试

- **单元测试**（49 个，`./gradlew :app:testDebugUnitTest`）：
  - 纯逻辑：`NextTriggerCalculator`、`StepDetector`、`ShakeDetector`、`MathChallenge`、`ChallengeEvaluator`、`TimerEngine`、`StopwatchEngine`、`PomodoroEngine`；
  - 仓库/调度：`AlarmRepositoryTest`（Turbine + 假 DAO/调度器，覆盖增删改与启用计数流）；
  - 界面逻辑：`RingingViewModelTest`（Robolectric，覆盖贪睡调度、步数/数学/摇晃挑战判定）；`FlipTimeTest`（翻页钟 24h/12h/秒显示拆分）。
- **仪器化 UI 测试**（`connectedDebugAndroidTest`，需模拟器/真机）：
  - `RingingScreenGatingTest`：响铃页步数不足时“关闭闹钟”禁用，达标后可用；
  - `AlarmEditChallengeRoundtripTest`：挑战类型/步数目标保存后回读一致。
- **静态检查**：`./gradlew :app:lintDebug`（0 error）。

## 验收对照表

| 验收项 | 如何验证 |
|---|---|
| 到点准时响铃 | 设置一个 1–2 分钟后的闹钟，锁屏等待；到点应全屏亮起并播放铃声（铃声渐强） |
| 步数挑战 | 响铃后不走动，“关闭闹钟”禁用并显示“还差 X 步”；走满目标步数后可关闭 |
| 数学题挑战 | 响铃页出现算术题，答对后“关闭闹钟”变为可用 |
| 摇晃挑战 | 用力摇晃手机达到次数后“关闭闹钟”变为可用 |
| 扫码挑战 | 相机取景框扫到任意二维码后可关闭；拒绝相机权限则回退为步数挑战 |
| 一次性闹钟自动停用 | 一次性闹钟响铃关闭后，列表卡片变为关闭态/不再触发 |
| 精确闹钟权限恢复 | 设置里授权后无需重开应用即恢复调度 |
| 闹钟守护智能启停 | 非小米默认无常驻通知；有启用闹钟后出现，删光后消失 |
| 计时器后台通知 | 计时器退到后台甚至清理后，到点有提示音通知 |
| AMOLED 主题 | 设置切到“纯黑（AMOLED）”后主界面背景为纯黑 |
| 中英文切换 | 设置 → 界面语言 切换立即生效 |

## 已知限制

- 不做“勿扰模式”自动解除（响铃时不覆盖 DND 静音）。
- 不做桌面小组件；“桌面时钟”为应用内全屏页面。
- 加速度计步数估算为简化算法，误差大于系统计步传感器，仅作为降级方案。
- 响铃步数统计以响铃页可见期间为准（传感器由前台服务持有，离开响铃页返回后继续累计）。
- 切换语言后，主界面立即生效；通知/服务等后台文案在进程重启后完全生效。
- 自定义主题颜色为 HSL 派生的近似 Material 3 配色（非官方色彩系统精确输出），视觉上保持统一。
- **QR 扫码挑战**依赖相机权限；未授权或相机不可用时自动回退为步数挑战。
- **闹钟守护（后台常驻）**：默认仅小米/红米设备开启，且只在“存在启用闹钟”时运行，无闹钟时自动停止（可在设置页手动开关）。
- 同一时刻多个闹钟响铃时，后到的闹钟不会打断当前响铃（避免用户被迫重新完成挑战）。
- **响铃全屏保证**：锁屏 / 灭屏 / 后台 / 勿扰 / 被清后台等设备开机状态下，响铃页都会全屏亮起并在响铃期间每 8 秒重新弹出（防止被系统或 OEM 关闭）；**真正关机（电源切断）后应用无法运行**，这是 Android 平台限制，普通应用无法在关机状态响铃。
- **HyperOS 全屏通知限制**：第三方应用拿不到系统闹钟的 `START_ACTIVITIES_FROM_BACKGROUND` 特权，锁屏全屏依赖 `setAlarmClock` 触发后的全屏通知白名单；小米开发者选项“系统优化”可能撤销该能力（只剩锁屏通知），需关闭该选项并重装应用，同时确认“后台弹出界面”已允许。
