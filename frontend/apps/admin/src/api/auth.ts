// 域/模块: 平台底座/认证授权（管理端调用侧）
// 类型: API 封装
// 职责: 登录/刷新/登出调用（契约见 docs/design/平台底座/detail/api/认证授权.md）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
import type { TokenPair } from "@slate/shared";
import { http } from "./http";

export const authApi = {
  /** POST /auth/login —— 后端实现随底座任务包交付，当前调用将收到传输错误/404 提示 */
  login(username: string, password: string): Promise<TokenPair> {
    return http.post("/auth/login", { username, password }) as Promise<TokenPair>;
  },
};
