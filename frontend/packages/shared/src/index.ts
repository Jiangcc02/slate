// 域/模块: 平台底座/前端共享包
// 类型: 包入口
// 职责: @slate/shared 对外导出（契约类型 + HTTP 客户端）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
export { ApiError, type PageQuery, type PageResult, type Result, type TokenPair } from "./types";
export { createHttp, ErrCode, type HttpOptions } from "./http";
