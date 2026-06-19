# 开发路线

## M0：工程骨架

- [x] README 双语切换
- [x] Android 工程基础配置
- [x] Compose 初始页面
- [x] Room 实体、DAO、Database
- [x] Repository + sync_outbox
- [x] Wi‑Fi 同步接口层

## M1：Todo MVP

- [ ] Todo ViewModel
- [ ] 今日任务列表
- [ ] 新建任务弹窗
- [ ] 完成/取消完成
- [ ] 优先级和截止日期
- [ ] Todo 变更写入 sync_outbox

## M2：番茄钟 MVP

- [ ] 番茄钟状态机
- [ ] 任务绑定
- [ ] 开始/暂停/结束
- [ ] 专注记录写入 Room
- [ ] 备用机 Board Mode 显示当前倒计时

## M3：记账 MVP

- [ ] 快速支出录入
- [ ] 分类选择
- [ ] 今日支出
- [ ] 本月支出
- [ ] 最近账单列表

## M4：备用机 Board Mode

- [ ] 横屏布局
- [ ] 竖屏布局
- [ ] Keep screen on
- [ ] 低亮度深色主题
- [ ] 防烧屏轻微位移
- [ ] 今日任务/番茄钟/支出三卡片

## M5：Wi‑Fi 同步 MVP

- [ ] Android NSD 广播 `_meaotodo._tcp`
- [ ] Android NSD 发现同局域网设备
- [ ] 手动配对码
- [ ] SyncBatch 发送
- [ ] SyncAck 确认
- [ ] 断线重试
- [ ] 同步状态 UI

## M6：体验增强

- [ ] 看板远程切换模式
- [ ] 支出预算提醒
- [ ] 周/月专注统计
- [ ] 数据导出
- [ ] 备份与恢复
