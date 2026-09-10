# internal/maintenance — 底座保留期清理任务

- 职责：只增不减表（登录日志/操作日志/消息投递/领域事件）的定时保留期清理。保留期与开关配置在 `slate.retention.*`（RetentionProperties）；MinIO 对象的回收在文件服务自己的 FileRetentionWorker。
- 维护者：协调者 / agent-fffabc
- 规则：
  - 清理各段独立 try/catch，单段失败不阻断其余段；
  - 只删除满足保留期的数据，DEAD 事件等需人工排查的数据不自动清理；
  - 新增可清理表时先补 `slate.retention` 配置项与默认值，再接入本 Worker。
