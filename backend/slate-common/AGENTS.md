# slate-common — 公共契约模型

> 职责：统一响应结构（`Result`）、错误码契约（`ErrorCode`）、分页模型（`PageQuery` / `PageResult`）——`contracts/api-conventions.md` §3/§4/§5 的代码形态。
> 规则：**无 Spring 依赖**；对外形状（字段名/结构）即 API 契约，改动属大档（design-guide §5），须先改契约文档。
> 维护者：协调者侧（平台底座领地）——交付署名 agent-fffabc
