# 十步闹钟 (Ten-Step Alarm)

一个用 **Kotlin + Jetpack Compose + Material 3** 编写的 Android 闹钟应用，包含三大功能：

1. **闹钟**：多闹钟管理（时间 / 星期重复 / 标签 / 铃声 / 音量 / 震动），AlarmManager + 前台服务保证准时响铃，**响铃后必须走动 10 步才能关闭**（系统计步传感器优先，加速度计估算降级）。
2. **番茄钟**：默认 25 分钟专注 / 5 分钟休息，时长可自定义，结束时有通知和提示音。
3. **翻页时钟**：应用内全屏翻页时钟摆件，屏幕常亮、沉浸式隐藏系统栏、轻点唤出按钮、横竖屏自适应。

界面为扁平化 Material 3 设计（圆角卡片、无渐变/厚重阴影），支持**中英文切换**、**跟随系统深浅色并可手动覆盖**、**主题颜色自定义**（动态取色 / 预设色板 / 自定义颜色）。

---

## 技术栈与版本

| 组件 | 版本 |
|---|---|
| 语言 | Kotlin 2.4.10（AGP 9 内置 Kotlin，Compose 编译器插件 2.4.10） |
| Gradle | 9.5.0（Wrapper） |
| Android Gradle Plugin | 9.3.0 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 |
| UI | Jetpack Compose BOM 2026.06.00（Compose 1.11.x，Material3 1.4.0） |
| 持久化 | Room 2.8.0（KSP 2.3.10）+ DataStore Preferences 1.1.7 |
| 导航 | Navigation Compose 2.9.0 |

> 说明：AGP 9 默认启用“内置 Kotlin”，因此工程**没有**应用 `org.jetbrains.kotlin.android` 插件；Kotlin 编译器版本通过根 `build.gradle.kts` 的 `buildscript` 固定为 2.4.10，与 Compose 编译器插件、KSP 保持一致。

---

## 如何构建运行

### 方式一：Android Studio（推荐）

1. 安装 **Android Studio 2026.x**（AI-261 及以上，支持 AGP 9）与 JDK 17+（AS 自带 JBR 即可）。
2. `File → Open`，选择本项目根目录（`settings.gradle.kts` 所在目录），等待 Gradle Sync 完成。
3. 首次同步会自动下载依赖；若提示缺少 SDK 组件，按提示安装 **Android SDK Platform 37** 与 **Build Tools 36.0.0**。
4. 连接真机或启动模拟器，点击 Run（绿色三角）即可安装运行。

### 方式二：命令行（使用本机 Android Studio 环境）

```bash
# 使用 Android Studio 自带的 JDK（JBR）与项目 wrapper：
# Windows PowerShell：
cd D:\Users\Desktop\New\alarm
& "D:\Program Files\Android\Android Studio\jbr\bin\java.exe" -jar .\gradle\wrapper\gradle-wrapper.jar :app:assembleDebug
& "D:\Program Files\Android\Android Studio\jbr\bin\java.exe" -jar .\gradle\wrapper\gradle-wrapper.jar :app:testDebugUnitTest

# 或设置 JAVA_HOME 后直接：
.\gradlew.bat :app:assembleDebug
# APK 输出位置：app/build/outputs/apk/debug/app-debug.apk
```

> 提示：`gradlew.bat` 对带空格的 JAVA_HOME 路径（如 `...\Android Studio\jbr`）校验较严格，若报 “invalid directory”，可直接用上面的 `java -jar gradle-wrapper.jar` 方式（等价于 AS 的构建环境）。

---

## 功能说明

### 闹钟
- **添加 / 编辑 / 删除**：主界面右下角 `+` 添加；点击卡片进入编辑页。可设置时间（24 小时制）、周一至周日多选重复、标签、铃声（系统铃声选择器）、音量（0–100%）、是否震动。
- **重复规则**：选了任意星期 → 每周按所选星期重复；**一个都不选 → 一次性闹钟**，响铃后自动停用。
- **苹果风格设定页**：闹钟编辑页采用 iOS 时钟风格——居中标题 + 取消/保存，**时分上下滚动转轮选择时间**，分组列表（重复 / 标签 / 铃声 / 音量 / 震动 / 稍后提醒 / 红色删除行）。
- **准时响铃**：`AlarmManager.setAlarmClock`（精确闹钟，状态栏显示闹钟图标）+ `AlarmRingingService` 前台服务（`specialUse` 类型）播放铃声/震动/保持 CPU，并发出带 `fullScreenIntent` 的高优通知拉起全屏响铃页。
- **勿扰模式也能响铃**：闹钟使用 `USAGE_ALARM` + `setAlarmClock`，响铃通知渠道开启 `setBypassDnd`，在勿扰模式下照常响起并全屏亮屏弹出。
- **强制步行关闭**：响铃页显示实时步数进度，**未满 10 步时“关闭闹钟”按钮禁用并显示“还差 X 步”**；满 10 步后按钮可用。
  - 优先使用系统计步传感器 `TYPE_STEP_COUNTER`（需要 `ACTIVITY_RECOGNITION` 运行时权限，首次启动会请求）；
  - 传感器缺失或权限被拒时，**自动降级为加速度计步数估算**，并在响铃页提示；
  - 两者都不可用时（例如无传感器设备），按钮保持禁用并提示原因。
- **稍后提醒 / 贪睡**：默认 5 分钟（设置页可改），可无限次；用 `setExactAndAllowWhileIdle` 调度，到点再次全屏响铃。
- **开机重建**：`BootReceiver` 监听 `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`，重启或更新后自动重建所有启用中的闹钟。
- **小米后台兼容**：设置页（小米/红米设备）提供「自启动 / 后台弹出界面 / 忽略电池优化」引导；响铃时接收器与服务双通道拉起全屏响铃页，并请求电池优化豁免，确保后台/息屏也能弹出。

### 权限
| 权限 | 用途 |
|---|---|
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | 精确闹钟（Android 12+；设置页有授权引导） |
| `USE_FULL_SCREEN_INTENT` | 全屏响铃（Android 14+，闹钟类应用默认授予） |
| `POST_NOTIFICATIONS` | 响铃 / 番茄钟通知（Android 13+ 运行时申请） |
| `ACTIVITY_RECOGNITION` | 系统计步传感器（Android 10+ 运行时申请；拒绝则用加速度计降级） |
| `VIBRATE` / `WAKE_LOCK` | 震动 / 响铃期间保持唤醒 |
| `RECEIVE_BOOT_COMPLETED` | 开机后重建闹钟 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | 响铃前台服务 |

### 番茄钟
- 专注（默认 25 分钟）/ 休息（默认 5 分钟）倒计时，可开始 / 暂停 / 重置 / 手动切换模式。
- 时长在设置页用滑块调整（专注 5–90 分钟，休息 1–30 分钟）。
- 阶段结束时播放系统通知音并弹出通知，自动切换到另一模式（暂停状态），专注轮数累计显示。
- **结束铃声可分别设置**：设置页可单独为“专注结束”与“休息结束”选择铃声；结束时播放铃声、发通知并弹出结束提示页，点击“好的”确认。

### 翻页时钟
- 入口：底部「时钟」Tab（首页顶部不再显示入口卡片）。
- 进入后**全屏直接显示时间字符**（无翻页卡片动画），大字号时:分（可加秒），横竖屏自适应、不锁定方向。
- **屏幕常亮**（`FLAG_KEEP_SCREEN_ON`），进入即隐藏状态栏与导航栏（沉浸式）。
- **3 秒自动沉浸**：进入页面（或点击唤出）3 秒后，除时间与其背景外的元素（返回按钮、秒开关、日期、星期）全部淡出；**轻点屏幕即可再次唤出**。
- **时间背景色可调**（设置 → 翻页时钟 → 时钟配色）：
  - 跟随主题（默认）：浅色主题 → 白底黑字；深色主题 → 黑底白字；
  - 浅色：固定白底黑字；深色：固定黑底白字；自定义：自行选择背景色，文字自动黑/白高对比。
- **全屏背景模式**：时钟页右上角按钮或设置项可开启全屏，背景色铺满整个屏幕、时间居中显示；**自适应大小**：时间始终上下左右居中，随屏幕尺寸自动缩放并保留边缘间距。
- 其它样式：显示秒 / 显示日期 / 24 小时制（或 12 小时制带上午/下午）/ 字号（70%–150%）。
### 语言与主题
- **界面语言**（设置 → 界面语言）：跟随系统 / 中文 / English，切换立即生效（UI 即时刷新；通知与后台服务文案在应用进程重启后完全生效）。
- **主题模式**（设置 → 主题模式）：跟随系统 / 浅色 / 深色，切换立即生效。
- **主题颜色**（设置 → 主题颜色）：
  - **动态取色**：Android 12+ 跟随系统壁纸取色（默认）；
  - **预设色板**：8 种内置配色（青 / 蓝 / 紫 / 绿 / 橙 / 红 / 粉 / 蓝灰）；
  - **自定义颜色**：HSL 三轴调色板自由选择，自动生成配套的浅色/深色整套配色。
  - 主题颜色对主界面与全屏响铃页同时生效。

---

## 验收对照表

| 验收项 | 如何验证 |
|---|---|
| Android Studio 可直接编译运行 | 打开工程 → Sync → Run（见上文） |
| 到点准时响铃 | 设置一个 1–2 分钟后的闹钟，锁屏等待；到点应全屏亮起并播放铃声 |
| 步行不足 10 步不能关闭 | 响铃后不走路，“关闭闹钟”按钮为禁用态，并显示“还差 X 步” |
| 走满 10 步后可关闭 | 拿起手机走动（可看步数进度），满 10 步按钮变可用，点击停止响铃 |
| 浅色/深色切换生效 | 设置页切换主题模式立即生效；“跟随系统”下改变系统主题也会跟随 |
| 中英文切换生效 | 设置 → 界面语言 切到中文/English 立即生效 |
| 主题颜色自定义生效 | 设置 → 主题颜色 选预设或自定义颜色后，主界面配色立即变化 |
| 番茄钟计时/通知/提示音正常 | 把专注时长调成 1 分钟并开始，结束时应有提示音和通知 |
| 翻页时钟 3 秒自动沉浸 | 进入时钟页，3 秒后日期/按钮自动淡出，仅剩时间与背景；轻点屏幕唤出 |
| 翻页时钟背景色可调 | 设置 → 翻页时钟 → 时钟配色：浅色主题默认白底黑字、深色主题黑底白字；自定义背景色立即生效 |

---

## 测试建议

- **单元测试**：`NextTriggerCalculatorTest`（下次触发时间计算）、`StepDetectorTest`（加速度计步数算法）、`PomodoroEngineTest`（番茄钟纯逻辑）。运行：`./gradlew :app:testDebugUnitTest`（或用 AS JBR：`java -jar gradle\wrapper\gradle-wrapper.jar :app:testDebugUnitTest`）。
- **真机 10 步验证**：需要一部带计步传感器的真机（绝大多数手机都有）。响铃时走动即可看到步数增长。
- **模拟器验证**：
  - 模拟器通常没有步进传感器，响铃页会提示“未检测到可用计步传感器”；
  - 在**设置 → 调试**中打开“响铃页显示‘模拟步数’按钮”，响铃时点该按钮 +10 步即可验收关闭流程（仅 Debug 构建可见）。
  - API 30+ 的模拟器可尝试 `adb emu sensor set step-counter <value>` 模拟计步值（部分镜像支持）。
- **精确闹钟**：Android 12+ 首次运行若系统拒绝精确闹钟，前往 **设置 → 精确闹钟权限** 引导授予。

---

## 项目结构（要点）

```
app/src/main/java/com/tenstep/alarm/
├── MainActivity.kt                  # 入口：edge-to-edge、权限请求、语言/主题装配
├── TenStepApplication.kt            # Application（语言注入）+ AppContainer（手动依赖容器）
├── alarm/                           # 闹钟核心
│   ├── NextTriggerCalculator.kt     # 下次触发时间纯计算
│   ├── AlarmScheduler.kt            # AlarmManager 封装（setAlarmClock/贪睡/取消）
│   ├── AlarmReceiver.kt             # 到点广播 → 前台服务 + 全屏页面 + 重复闹钟重调度
│   ├── BootReceiver.kt              # 开机/更新后重建闹钟
│   ├── AlarmRingingService.kt       # 响铃前台服务（铃声/震动/唤醒/计步）
│   ├── StepDetector.kt              # 加速度计步数算法（纯逻辑）
│   ├── StepGate.kt                  # 计步传感器选择与监听
│   └── RingingSession.kt            # 响铃状态共享
├── data/                            # Room(AlarmEntity/DAO/DB) + DataStore(SettingsStore)
├── pomodoro/PomodoroViewModel.kt    # 番茄钟状态机 + 纯逻辑引擎
├── ui/
│   ├── theme/                       # M3 主题（动态取色/预设/自定义色 + HSL 色板工具）
│   ├── navigation/AppNavigation.kt  # 底部导航 + NavHost
│   ├── alarm/                       # 闹钟列表 / 编辑页
│   ├── ringing/                     # 全屏响铃页（10 步门槛）
│   ├── pomodoro/                    # 番茄钟页
│   ├── clock/                       # 翻页时钟页（FlipDigit 动画 + 样式设置）
│   └── settings/                    # 设置页（语言/主题颜色/时钟样式/时长/权限/调试）
└── util/                            # 通知渠道 + 应用内语言切换（LocaleHelper）
```

## 已知限制

- 不做“勿扰模式”自动解除（响铃时不覆盖 DND 静音）。
- 不做桌面小组件；“桌面时钟”为应用内全屏页面。
- 加速度计步数估算为简化算法，误差大于系统计步传感器，仅作为降级方案。
- 响铃步数统计以响铃页可见期间为准（传感器由前台服务持有，离开响铃页返回后继续累计）。
- 切换语言后，主界面立即生效；通知/服务等后台文案在进程重启后完全生效。
- 自定义主题颜色为 HSL 派生的近似 Material 3 配色（非官方色彩系统精确输出），视觉上保持统一。
