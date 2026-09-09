// 域/模块: 平台底座/前端共享包
// 类型: 契约类型
// 职责: api-conventions 的前端形态——统一响应/分页结构与错误码语义（§3/§4/§5）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc

/** 统一响应结构（api-conventions §3）：code=0 成功；traceId 与日志链路一致 */
export interface Result<T> {
  code: string;
  message: string;
  data: T;
  traceId: string;
}

/** 分页请求参数（api-conventions §5）：page 1 起、size ≤100、sort 如 "field,desc" */
export interface PageQuery {
  page?: number;
  size?: number;
  sort?: string[];
}

/** 分页响应（api-conventions §3）：分页接口 data 的固定形态 */
export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}

/** 登录态双 token（api-conventions §2 定稿：access 2h + refresh 7d 滚动） */
export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

/** 业务错误：携带契约错误码（如 AUTH-001），前端按 code 映射用户文案（api-conventions §4） */
export class ApiError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly traceId?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}
