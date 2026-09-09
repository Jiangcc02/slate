# frontend/ — 前端工程（Vue 3 pnpm monorepo）

> 遵守根 `AGENTS.md` 与 `docs/architecture/iron-laws.md`；技术基线见蓝图 §十三（Vue 3 + TypeScript + Pinia + Element Plus + ECharts）。

## 结构

| 位置 | 是什么 | 交付期 |
|---|---|---|
| `apps/admin` | 管理端（教师/教务/管理员/校长端） | 一期起 |
| `apps/student` | 学生端 | 一期起 |
| `apps/classroom` | 教室端大屏（Web 教室端专题） | 四期（占位） |
| `packages/shared` | 共享包：契约类型（对齐 api-conventions）、HTTP 客户端（统一响应解包/traceId/401 处理）、工具 | 持续 |

## 本目录规则

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
