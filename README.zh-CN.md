# MeaoToDo

[中文](./README.zh-CN.md) | [English](./README.en.md)

MeaoToDo 是一个 Android 单机、本地优先的个人助手。它把任务、专注、账本和每日复盘连接成一条完整工作流，并在用户自行配置 API Key 后提供可控的 AI 辅助。

## 当前可用能力

- Todo：智能视图、自定义清单、搜索、筛选、排序、日历、任务编辑/移动/复制/归档。
- 智能速记：支持 `#今天`、`#明天`、`!高`、`!中`、`!低`、`@清单名`、`🍅3`。
- 番茄钟：多轮专注/休息、暂停/继续/跳过/放弃、任务绑定和历史汇总。
- 账本：精确到分的本地支出录入、日/月统计、分类概览和预算节奏。
- 洞察：专注推荐、效率脉搏、连续专注天数和 7 日趋势。
- 助手：统一日上下文、本地命令解析、待确认动作、日时间线和全局专注条。
- BYOK AI：加密 Key 设置、兼容 API 接入、任务草稿、每日建议、晚间复盘和计划调整。
- 自动化：可选的每日建议和晚间复盘，每天最多触发一次。
- 控制：待确认动作可编辑，并提供每日请求与每月 Token 上限。

## 正在建设

- 提示词离线评测集和更多异常供应商兼容性测试。
- 更完整的 Compose UI 与 Room migration instrumentation 测试。
- 数据导出、备份与隐私控制。

## AI 安全边界

- API Key 仅保存在本机，并使用 Android Keystore 保护。
- 模型输出先转换为结构化草稿或待确认动作。
- 新建、修改、删除、延期、记账等数据操作都需要用户确认。
- 默认只发送完成当前能力所需的最小上下文。
- 没有 API 或网络时，本地任务、专注、账本和确定性速记继续工作。

## 技术栈

- Kotlin / Java 17
- Jetpack Compose
- Room
- DataStore
- Kotlin Coroutines / Flow
- Kotlin Serialization

## 项目结构

```text
app/src/main/java/com/kdlay/meaotodo/
  core/           应用容器、设置与通用工具
  data/           Room 实体、DAO、Repository
  domain/         跨模块上下文、动作和用例
  ai/             AI 接入、提示词、结构化输出与校验
  ui/             Compose 页面和主题
```

## 本地运行

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

## 设计原则

- 本地优先：核心功能不依赖网络或 AI。
- 确认后执行：助手建议不会在后台悄悄修改数据。
- 跨模块闭环：任务可以进入专注，实际投入会进入时间线和复盘。
- 少而有效：建议应有依据、可解释、可撤销。
- 渐进增强：先完成可验证的纵向闭环，再扩大自动化范围。
