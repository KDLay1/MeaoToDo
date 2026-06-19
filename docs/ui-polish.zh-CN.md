---
title: ToDo UI Polish 设计说明
created: 2026-06-19
status: draft
module: todo-ui
tags:
  - Android
  - Jetpack-Compose
  - UI
  - Todo
---

# ToDo UI Polish 设计说明

## 1. 本轮目标

本轮不是新增业务能力，而是给 MeaoToDo 建立第一版视觉基线。

主要目标：

```text
1. 让 ToDo 主界面从占位页面变成接近正式 App 的样子
2. 建立全局颜色、圆角、卡片、导航的视觉规则
3. 为后续真实 Room-backed ToDo 列表接入预留组件结构
4. 为 Board Mode 的深色低亮度风格预留方向
```

暂不处理：

```text
1. Repository / DAO / Room 数据逻辑
2. 真实任务新增、编辑、删除交互
3. 真实番茄钟状态机
4. 真实记账数据
5. Wi-Fi Sync 真实状态
```

---

## 2. 视觉方向

本轮参考滴答清单的信息组织方式，但不照搬它的商务效率工具风格。

MeaoToDo 的方向是：

> 清晰、温和、低阻力、有一点生活看板气质。

关键词：

```text
奶油色背景
蓝紫主色
橙粉强调
低饱和绿色完成态
大圆角
轻阴影
清单 Chip
轻量任务卡片
```

---

## 3. 已调整内容

## 3.1 Theme

文件：

```text
app/src/main/java/com/kdlay/meaotodo/ui/theme/Theme.kt
```

调整内容：

```text
1. 完善 lightColorScheme
2. 完善 darkColorScheme
3. 增加 primaryContainer / secondaryContainer / tertiaryContainer
4. 增加 surfaceVariant / outline / errorContainer
5. 增加统一 Shapes
```

目的：后续 Pomodoro、Ledger、Board Mode 不要各自写临时颜色。

---

## 3.2 App Shell

文件：

```text
app/src/main/java/com/kdlay/meaotodo/ui/MeaoTodoApp.kt
```

调整内容：

```text
1. 底部导航视觉更轻
2. Today / Timer / Ledger / Board 四个入口保留
3. Scaffold 使用全局 background
4. Board Mode 使用单独深色预览
```

---

## 3.3 Today 页面

当前 Today 页面是 UI preview，不直接接入 Repository。

包含组件：

```text
TodayHero
ListSwitcher
QuickAddBar
FocusTaskCard
SectionHeader
TaskRow
CompletedToggle
EmptyTodoState
```

这些组件后续可以迁移到：

```text
app/src/main/java/com/kdlay/meaotodo/ui/todo/
```

建议下一轮再拆文件，避免本轮 PR 过大。

---

## 4. 后续接入真实 ToDo 数据的建议

当 ViewModel / Repository 已经稳定后，可以按下面方式迁移：

```text
TaskEntity / Domain Task
        ↓
TodoUiState
        ↓
UiTask
        ↓
TodoPolishScreen
```

建议保留一个 UI mapper：

```kotlin
fun TaskEntity.toUiTask(): UiTask
```

这样 UI 不直接依赖 Room Entity，后续增加清单、标签、重复任务时更容易维护。

---

## 5. Codex 复核建议

请 Codex 优先检查：

```text
1. ./gradlew assembleDebug 是否通过
2. Compose 是否有编译错误
3. Theme.kt 的 Material3 colorScheme / Shapes 是否兼容当前依赖版本
4. MeaoTodoApp.kt 是否有明显不可用 API
5. UI 是否只影响展示层，没有破坏 data / repository / sync
```

本轮没有在本地运行 Android 构建，因此最终构建结果需要以 Codex 本地测试为准。
