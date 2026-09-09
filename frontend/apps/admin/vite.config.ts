// 域/模块: 平台底座/管理端应用配置
// 类型: Vite 配置
// 职责: 管理端构建与开发代理（/api → 本地 slate-boot:8080）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
