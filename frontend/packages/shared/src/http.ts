// 域/模块: 平台底座/前端共享包
// 类型: HTTP 客户端
// 职责: 统一请求封装——Result 解包（code≠0 抛 ApiError）、traceId 透出、401 未授权回调、token 注入
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
import axios, { type AxiosInstance } from "axios";
import { ApiError, type Result } from "./types";

export interface HttpOptions {
  /** 401/token 失效（AUTH-004）时触发，应用注入跳登录逻辑 */
  onUnauthorized?: () => void;
  /** 取 accessToken 的函数（登录后由应用注册） */
  getAccessToken?: () => string | undefined;
}

/** 错误码语义见 api-conventions §4（一经发布冻结） */
export const ErrCode = {
  UNAUTHORIZED: "AUTH-004",
} as const;

export function createHttp(baseURL = "/api/v1", options: HttpOptions = {}): AxiosInstance {
  const http = axios.create({ baseURL, timeout: 15_000 });

  http.interceptors.request.use((config) => {
    const token = options.getAccessToken?.();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  http.interceptors.response.use(
    (response) => {
      const body = response.data as Result<unknown>;
      if (body.code === "0") {
        return body.data as never;
      }
      if (body.code === ErrCode.UNAUTHORIZED) {
        options.onUnauthorized?.();
      }
      return Promise.reject(new ApiError(body.code, body.message, body.traceId));
    },
    (error) => {
      // 传输层错误：HTTP 状态码表达（api-conventions §3），尽量还原 body 中的错误码
      const body = error.response?.data as Result<unknown> | undefined;
      if (body && typeof body.code === "string" && body.code !== "0") {
        return Promise.reject(new ApiError(body.code, body.message, body.traceId));
      }
      return Promise.reject(
        new ApiError(error.response?.status ? `HTTP-${error.response.status}` : "NETWORK", error.message),
      );
    },
  );

  return http;
}
