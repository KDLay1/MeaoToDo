# 架构说明

## 总体目标

MeaoToDo 是单设备、本地优先的个人助手。任务、专注和账本始终以 Room 为事实来源；助手层组合这些本地数据，生成可解释建议和待确认动作。AI 是可选增强，不是核心功能运行的前提。

## 分层

```text
Compose UI
  ↓
Assistant / Domain Use Cases
  ↓                         ↘
Repository → Room             AI Gateway（可选）
  ↓                             ↓
Local change log             用户配置的 API
```

## 本地编排

- `DailyContext` 汇总今日任务、逾期任务、专注状态、账本摘要和预算节奏。
- `PendingAction` 统一描述新建任务、延期、启动专注和记账等候选操作。
- `AssistantContextBuilder` 只提取当前能力需要的最小上下文。
- 本地确定性规则承担快速录入、基础建议和离线回退。

## AI 边界

- API Key 使用 Android Keystore 保护，不写入源码、日志或导出。
- AI 接入层与供应商解耦，优先兼容 Base URL + Model + API Key 的通用接口。
- 提示词按能力版本化维护，模型必须返回受约束的结构化结果。
- 结构化结果经过解析和业务校验后转成 `PendingAction`。
- 任何本地数据变更都需要用户确认。

## sync_outbox 的现阶段定位

`sync_outbox` 是早期双机同步遗留。当前先保留为本地变更日志，避免在产品重构同时引入数据库迁移风险。它不再连接 Wi-Fi 发现或传输服务，也不再对用户承诺跨设备同步。后续可在独立迁移中删除，或改造成撤销/操作审计基础。

## 当前模块

- `data/local/entity`：Room 表结构。
- `data/local/dao`：数据库访问接口。
- `data/repository`：本地业务写入入口。
- `domain`：跨模块上下文、建议、动作和用例。
- `ai`：密钥、API 客户端、提示词、结构化输出和校验。
- `ui`：助手、计划、记录和二级设置。

## 实施顺序

1. 清理双机产品与运行时遗留。
2. 建立 `DailyContext`、通用输入和统一动作协议。
3. 接入 BYOK AI 基础设施。
4. 完成任务草稿、每日简报、晚间复盘和计划调整闭环。
5. 围绕助手工作流重构一级与二级 UI。
6. 完善测试、隐私、成本控制和提示词评测。
