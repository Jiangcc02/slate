# slate-platform — 平台底座域

> 职责：认证授权（JWT/RBAC/审计）、组织架构、用户中心（含家长-学生绑定）、消息中心、文件服务、系统管理。设计见 `docs/design/平台底座/design.md`。
> 规则：
> - **包结构**：对外契约（接口/DTO）只放 `com.slate.platform.api` 及其子包；其余全部在 `com.slate.platform.internal`——他域 import internal 包会被 ArchUnit 打回（铁律 L7）
> - 底座是共享领地：业务域不得为迁就自己改本 module；变更须由底座任务包 Issue 承载
> - 子模块分包：`api/auth` `internal/auth`、`api/org` `internal/org`……随实现任务逐包建立
> 维护者：协调者侧（平台底座领地）——交付署名 agent-fffabc
