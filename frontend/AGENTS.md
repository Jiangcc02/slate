# frontend/ — 前端工程（Vue 3 pnpm monorepo）

> 遵守根 `AGENTS.md` 与 `docs/architecture/iron-laws.md`；技术基线见蓝图 §十三（Vue 3 + TypeScript + Pinia + Tailwind CSS v4 + shadcn-vue 组件源码模式 + ECharts）。

## 结构

| 位置 | 是什么 | 交付期 |
|---|---|---|
| `apps/admin` | 管理端（教师/教务/管理员/校长端） | 一期起 |
| `apps/student` | 学生端 | 一期起 |
| `apps/classroom` | 教室端大屏（Web 教室端专题） | 四期（占位） |
| `packages/shared` | 共享包：契约类型（对齐 api-conventions）、HTTP 客户端（统一响应解包/traceId/401 处理）、工具 | 持续 |

## 本目录规则

- **UI 组件 = shadcn-vue 源码复制模式**（shadcn/ui 官方为 React，本项目用其 Vue 3 移植 shadcn-vue：Reka UI 原语 + Tailwind CSS v4）：
  - 组件源码在 `apps/<app>/src/components/ui/`，用官方 CLI 按需引入：`pnpm dlx shadcn-vue@latest add <组件>`（在对应 app 目录执行；产物须补 L1 头，见 CI 脚本）
  - 组件源码保持上游一致，不手改；定制走主题令牌（`src/assets/main.css` 的 CSS 变量）与外层包装
  - **禁止引入整包组件库**（Element Plus / Ant Design Vue 等）；图标用 lucide-vue-next
  - 三应用主题令牌保持一致；教室端大屏主题随四期专题定
- **包管理只用 pnpm**（workspace：`apps/*` + `packages/*`）；依赖版本统一在 root `package.json` / 各包声明，`pnpm-lock.yaml` 入库，安装用 `pnpm install --frozen-lockfile`
- **契约对齐**：`packages/shared` 里的类型与 HTTP 行为是 api-conventions 的前端形态——后端契约变更必须同步这里（大档）
- **跨应用复用走 shared**：三应用公共代码（api 客户端、类型、布局组件）提入 `packages/shared` 或新建 `packages/*`，禁止应用间互相引用源码
- 每个提交进来的 `.ts` / `.vue` 文件必须带 L1 文件头（CI 脚本 `scripts/check-file-headers.sh` 强制）
- 开发代理：各应用 vite dev server 已配 `/api` → `http://localhost:8080`（后端 slate-boot）

## 常用命令（frontend/ 下）

| 命令 | 用途 |
|---|---|
| `pnpm dev:admin` / `pnpm dev:student` | 启动对应应用开发服务器 |
| `pnpm lint` / `pnpm type-check` / `pnpm build` | CI 同款三关卡 |

## 维护者

协调者侧（平台底座领地：骨架与 shared）——交付署名 agent-fffabc；各业务页面随所属域任务包交付（任务包默认完整纵切，见 git-workflow 与蓝图 §十五）
