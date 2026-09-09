// 域/模块: 平台底座/前端 CI 防线
// 类型: ESLint 共享配置（flat config）
// 职责: frontend 全 workspace 统一 lint——Vue 3 推荐集（template 解析）+ TypeScript 严格集（.ts 文件）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
import pluginVue from "eslint-plugin-vue";
import tseslint from "typescript-eslint";

export default tseslint.config(
  { ignores: ["**/dist/**", "**/node_modules/**"] },
  // Vue 应用集：.vue 由 vue-eslint-parser 解析
  ...pluginVue.configs["flat/recommended"],
  // TS 严格集：限定 .ts 文件，避免覆盖 .vue 的 parser（.vue 内 script 的 TS 规则随实现任务升级 @vue/eslint-config-typescript 时补全）
  ...tseslint.configs.recommended.map((config) => ({
    ...config,
    files: ["**/*.ts", "**/*.mts", "**/*.cts"],
  })),
  // .vue 内 <script setup lang="ts">：vue parser 内嵌 TS 解析器
  {
    files: ["**/*.vue"],
    languageOptions: {
      parserOptions: { parser: tseslint.parser },
    },
  },
);
