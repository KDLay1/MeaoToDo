# MeaoToDo

[中文](./README.zh-CN.md) | [English](./README.en.md)

MeaoToDo 是一个面向 Android 双机协同的个人效率 APP。它的核心设定是：

- **主力机**：快速录入 Todo、番茄钟、记账数据。
- **备用机**：通过同一 Wi‑Fi 局域网同步数据，作为常亮桌面看板显示今日任务、当前番茄钟和消费概况。

## 为什么做这个项目

很多效率 APP 只假设用户有一台手机，但这个项目的目标是充分发挥备用机价值：让备用机从“吃灰设备”变成桌面上的低干扰生活仪表盘。

## 第一阶段目标

- TodoList：任务创建、完成、今日任务、优先级、截止日期。
- 番茄钟：开始、暂停、结束、绑定任务、记录专注历史。
- 记账：快速记录支出、分类、今日支出、本月支出。
- 看板模式：大字号、低亮度、横竖屏适配、防烧屏位移预留。
- Wi‑Fi 同步：先实现局域网设备发现、手动配对、增量数据交换的框架。

## 暂不实现

- 云同步
- 多用户协作
- AI 账单识别
- 复杂日历
- iOS / Web / 桌面端

## 技术路线

- Kotlin
- Jetpack Compose
- Room
- DataStore
- Android NSD / 局域网通信接口层
- 本地优先的数据结构

## 双机工作流

```text
小米 15 主力机
  ├─ 快速添加 Todo
  ├─ 开始番茄钟
  ├─ 快速记账
  └─ 通过 Wi‑Fi 向备用机推送增量数据

小米 10s 备用机
  ├─ 进入 Board Mode
  ├─ 常亮显示今日任务
  ├─ 显示当前番茄钟
  └─ 显示今日/本月消费概况
```

## 项目结构

```text
app/src/main/java/com/kdlay/meaotodo/
  core/           应用容器、通用工具
  data/           Room 实体、DAO、Repository
  domain/         业务模型和用例
  sync/           Wi‑Fi 发现、配对、同步协议
  ui/             Compose 页面和主题
```

## 开发顺序

1. 搭好 Android 工程骨架。
2. 完成 Todo 的本地数据库与页面。
3. 完成番茄钟状态机与记录。
4. 完成记账的最小录入与统计。
5. 完成 Board Mode。
6. 实现 Wi‑Fi 发现与手动配对。
7. 实现增量同步。

## 本地运行

当前仓库不提交 Gradle Wrapper 的二进制 jar。第一次使用时可用 Android Studio 打开工程，或在本地执行：

```bash
gradle wrapper
./gradlew :app:assembleDebug
```

如果你使用 Windows PowerShell：

```powershell
.\gradlew.bat :app:assembleDebug
```

## 设计原则

- 本地优先：没有网络也能记录任务、番茄钟和账单。
- 同步克制：Wi‑Fi 同步只交换必要增量，不做复杂云账号系统。
- 备用机优先：看板模式不是普通页面放大版，而是专门为常亮展示设计。
- 先小后大：先做每天能用的 MVP，再逐步加入课表、习惯追踪等模块。
