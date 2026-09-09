# slate-framework — 工程规范机制

> 职责：Spring 基础设施件——全局异常处理（`GlobalExceptionHandler`）、traceId 日志链路（`TraceIdFilter`）、雪花 ID 生成器（`SnowflakeIdGenerator`，主链 ID 契约的统一生成器）。幂等、审计、领域事件外发机制随底座实现任务补入本 module。
> 规则：本 module 是全系统共享地基（铁律 L7）——业务域不得为迁就自己改这里，变更须由底座任务包 Issue 承载；不得出现任何业务语义。
> 维护者：协调者侧（平台底座领地）——交付署名 agent-fffabc
