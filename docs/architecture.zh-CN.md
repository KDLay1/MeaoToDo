# 架构说明

## 总体目标

MeaoToDo 采用本地优先架构。所有 Todo、番茄钟、记账数据都先写入本机 Room 数据库，再由 Wi‑Fi 同步层把增量变更发送给局域网内的另一台手机。

## 分层

```text
UI / Compose
  ↓
Repository
  ↓
Room DAO
  ↓
Room Database

Sync Gateway
  ↓
Sync Outbox
  ↓
Wi‑Fi Transport
```

## 为什么使用 sync_outbox

直接在每次用户操作时立即同步，会让业务逻辑和网络状态强绑定。使用 `sync_outbox` 后，业务层只负责记录“发生了什么变化”，同步层可以在稍后统一发送。

优点：

- 离线时不丢数据。
- 同步失败可以重试。
- Wi‑Fi 同步可以批量发送。
- 主力机和备用机都可以用同一套数据模型。

## 当前模块

- `data/local/entity`：Room 表结构。
- `data/local/dao`：数据库访问接口。
- `data/repository`：业务写入入口，并负责生成同步 outbox。
- `sync`：设备发现、同步批次、ACK 的接口层。
- `ui`：初始 Compose 页面。

## 下一步

1. 把 UI 接入 Repository。
2. 为 Todo 增加 ViewModel。
3. 实现真实 JSON payload。
4. 使用 Android NSD 发现 `_meaotodo._tcp` 服务。
5. 实现局域网 HTTP 或 Socket 传输。
6. 在备用机 Board Mode 中订阅同步结果。
