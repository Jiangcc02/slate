# quickstart — 五分钟把 slate 跑起来

> 面向新成员（人与 Agent）的本地启动指南。项目全貌与文档导航见 [docs/codemap.md](docs/codemap.md)。

## 1. 你要跑的是什么

slate = K12 智慧教育平台（教/学/练/测/评全链路，六端）。当前处于**一期建设起点**：

- 后端：`backend/`，Spring Boot 3 模块化单体（Maven 多 module，JDK 17）
- 前端：`frontend/`，Vue 3 pnpm monorepo（管理端 + 学生端，shadcn-vue / Tailwind CSS v4）
- 数据库等基础设施（MySQL 8 / Redis / MinIO）**当前骨架阶段还不需要**——随底座实现任务包引入（届时在此文档更新 docker-compose 说明）

## 2. 环境要求

| 工具 | 版本 | 检查命令 | 安装指引 |
|---|---|---|---|
| JDK | 17+ | `java -version` | https://adoptium.net/temurin/releases/?version=17 |
| Maven | 3.9+ | `mvn -version` | https://maven.apache.org/download.cgi |
| Node.js | 20+ | `node -v` | https://nodejs.org/zh-cn/download |
| pnpm | 9+ | `pnpm -v` | `corepack enable` 或 `npm install -g pnpm` |

## 3. Windows 一键启动

| 脚本 | 作用 | 用法 |
|---|---|---|
| `scripts\start-backend.cmd` | 启动后端 slate-boot（:8080） | 双击或命令行运行；`--check` 仅自检环境 |
| `scripts\start-frontend.cmd` | 启动管理端（:5173）+ 学生端（:5174），各占一个窗口 | 同上；首次自动 `pnpm install` |

脚本会自动探测 JDK/Maven（含本机 `~\.zcode\tools\` 工具目录兜底），探测失败会给出安装指引。

## 4. 手动启动（非 Windows / 不用脚本）

```bash
# 后端（JDK 17 + Maven）
cd backend
mvn -pl slate-boot -am spring-boot:run

# 前端（两个终端各起一个；首次先在 frontend/ 下 pnpm install）
cd frontend
pnpm --filter @slate/admin dev      # 管理端 http://localhost:5173
pnpm --filter @slate/student dev    # 学生端 http://localhost:5174
```

## 5. 验证跑通了

1. 后端健康检查：`curl http://localhost:8080/actuator/health` → `{"status":"UP"}`
2. 打开 http://localhost:5173 与 http://localhost:5174，应看到各自登录页（shadcn 风格卡片表单）
3. 登录页提交报「网络异常」或 `AUTH-xxx` 错误 = **符合预期**：认证接口随底座实现任务包交付，当前骨架阶段调用链只到统一错误提示

## 6. 常见问题

| 现象 | 处理 |
|---|---|
| 后端脚本提示找不到 JDK 17 | 安装 Temurin 17，或解压到 `%USERPROFILE%\.zcode\tools\jdk-17.0.2` |
| `pnpm install` 报构建脚本被忽略 | 已由 `pnpm-workspace.yaml` 白名单处理（esbuild / vue-demi）；新增依赖触发时按提示审查后加白名单 |
| 端口占用（8080/5173/5174） | 改 `backend/slate-boot/src/main/resources/application.yml` 的 `server.port`，或各 app 的 `vite.config.ts` |
| CI 在哪看 | `.github/workflows/`：后端（编译+测试+ArchUnit+L1 头检查）与前端（lint+type-check+build+头检查）两条防线，PR→dev 时自动跑 |

## 7. 然后去哪

- 提交代码前：读根目录 `AGENTS.md` → [docs/codemap.md](docs/codemap.md) → [docs/product/core.md](docs/product/core.md) → [docs/architecture/iron-laws.md](docs/architecture/iron-laws.md)
- 领任务：[docs/guides/git-workflow.md](docs/guides/git-workflow.md)（分支/PR/Issue 硬性约束）
- 底座设计（当前实现目标）：[docs/design/平台底座/design.md](docs/design/平台底座/design.md)
