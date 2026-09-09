# packages/shared — 前端共享包

> 职责：三应用公共代码——契约类型（`types.ts`，api-conventions 的前端形态）、HTTP 客户端（`http.ts`，统一响应解包 / traceId / 401 处理）、跨端工具。
> 规则：本包是前端侧的契约对齐层——后端契约（api-conventions）变更必须同步本包类型与解包行为（大档）；禁止放任何业务页面逻辑；应用不得绕过本包自造请求封装。
> 维护者：协调者侧（平台底座领地）——交付署名 agent-fffabc
