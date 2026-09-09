// 域/模块: 平台底座/管理端应用
// 类型: 应用 HTTP 实例
// 职责: 管理端请求客户端——token 注入与 401 跳登录（契约行为见 @slate/shared）；双 token 本地存储
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
import { createHttp } from "@slate/shared";
import router from "../router";

const ACCESS_TOKEN_KEY = "slate.admin.accessToken";
const REFRESH_TOKEN_KEY = "slate.admin.refreshToken";

export const http = createHttp("/api/v1", {
  getAccessToken: () => localStorage.getItem(ACCESS_TOKEN_KEY) ?? undefined,
  onUnauthorized: () => {
    clearTokens();
    router.push({ name: "login" });
  },
});

export function saveTokenPair(accessToken: string, refreshToken: string) {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export function getAccessToken(): string | undefined {
  return localStorage.getItem(ACCESS_TOKEN_KEY) ?? undefined;
}

export function getRefreshToken(): string | undefined {
  return localStorage.getItem(REFRESH_TOKEN_KEY) ?? undefined;
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}
