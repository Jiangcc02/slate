// 域/模块: 平台底座/学生端应用配置
// 类型: Vite 配置
// 职责: 学生端构建、Tailwind CSS v4 编译、@ 路径别名、开发代理（/api → 本地 slate-boot:8080）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import tailwindcss from "@tailwindcss/vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    port: 5174,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
