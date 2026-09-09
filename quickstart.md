# quickstart — 五分钟把 slate 跑起来

> 面向新成员（人与 Agent）的本地启动指南。项目全貌与文档导航见 [docs/codemap.md](docs/codemap.md)。

## 1. 你要跑的是什么

slate = K12 智慧教育平台（教/学/练/测/评全链路，六端）。当前处于**一期建设起点**：

- 后端：`backend/`，Spring Boot 3 模块化单体（Maven 多 module，JDK 17）
- 前端：`frontend/`，Vue 3 pnpm monorepo（管理端 + 学生端，shadcn-vue / Tailwind CSS v4）
- 基础设施（本地开发必需）：MySQL 8（:3306，库 `slate` 由应用启动时自动创建并建表/种子）+ Redis（:6379）+ MinIO（:9000 API / :9001 控制台）——Redis/MinIO 在仓库根 `docker compose up -d` 一键起；MySQL 建议本机安装或自行容器化
- MySQL 凭证（不入仓库，每人自备）：把 `backend/slate-boot/src/main/resources/application-local.yml.example` 复制为同目录 `application-local.yml` 并填入本机账号密码；或设环境变量 `SLATE_MYSQL_USER` / `SLATE_MYSQL_PASSWORD`，两种方式任选其一
- 未就绪：外部通知渠道（邮件/短信/微信仅预留接口）、WebSocket 实时推送（随三期随堂测引入）

## 2. 环境要求

> 这些工具都是**你电脑上的开发环境**（安装到系统或解压到本机任意目录均可），不随项目仓库分发——仓库里不含也不应放任何 JDK / Node 运行时。

| 工具 | 版本 | 检查命令 | 安装指引 |
|---|---|---|---|
| JDK | 17+ | `java -version` | https://adoptium.net/temurin/releases/?version=17 |
| Maven | 3.9+ | `mvn -version` | https://maven.apache.org/download.cgi |
| Node.js | 20+ | `node -v` | https://nodejs.org/zh-cn/download |
| pnpm | 9+ | `pnpm -v` | `corepack enable` 或 `npm install -g pnpm` |

解压版（免安装）用户：把 JDK / Maven 解压到本机任意目录，然后设置环境变量 `SLATE_JDK17`、`SLATE_M2` 指向对应解压目录即可，启动脚本会优先使用它们。

## 3. Windows 一键启动

| 脚本 | 作用 | 用法 |
|---|---|---|
| `scripts\start-backend.cmd` | 启动后端 slate-boot（:8080） | 双击或命令行运行；`--check` 仅自检环境 |
| `scripts\start-frontend.cmd` | 启动管理端（:5173）+ 学生端（:5174），各占一个窗口 | 同上；首次自动 `pnpm install` |

脚本会自动探测**你电脑上**的 JDK/Maven。JDK 顺序：`SLATE_JDK17` 环境变量 → `JAVA_HOME` → 个别机器的本地约定目录，选中的会进一步验证主版本确实是 17+（`JAVA_HOME` 指着 JDK 8 这类情况会在探测期被拦下并给出指引）；Maven 顺序：`SLATE_M2` 环境变量 → 本地约定目录 → PATH（PATH 上每个候选都会先验证能真正运行才采用；最终选中的以完整路径调用，不受运行时二次解析影响）。本地约定目录仅协调者电脑存在，其他机器探测落空后自动尝试下一级、全部落空则给出安装指引。
注意两点：脚本输出为**英文**（cmd 批处理对非 ASCII 内容有解析兼容问题，中文说明以本文档为准）；出错或结束时窗口会**停留**（`pause`），报错信息不会被闪退吞掉。

## 4. 手动启动（非 Windows / 不用脚本）

```bash
# 后端（JDK 17 + Maven）：先打包再运行（多模块下不要直接对父 POM 跑 spring-boot:run）
cd backend
mvn -pl slate-boot -am package -DskipTests
java -jar slate-boot/target/slate-boot-*.jar

# 前端（两个终端各起一个；首次先在 frontend/ 下 pnpm install）
cd frontend
pnpm --filter @slate/admin dev      # 管理端 http://localhost:5173
pnpm --filter @slate/student dev    # 学生端 http://localhost:5174
```

## 5. 验证跑通了

1. 后端健康检查：`curl http://localhost:8080/actuator/health` → `{"status":"UP"}`（首次启动自动建库建表+种子）
2. 登录闭环：`curl -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'` → 返回双 token（种子账号：admin/admin123 管理员、teacher01/teacher02 密码 teacher123 教师）
3. 打开 http://localhost:5173 与 http://localhost:5174，用 **admin / admin123** 登录（登录成功即通；页面级的登录后首页随后续任务包交付）

## 6. 常见问题

| 现象 | 处理 |
|---|---|
| 后端脚本提示找不到 JDK 17 | 安装 Temurin 17 并配置 `JAVA_HOME`，或解压到本机任意目录后设置 `SLATE_JDK17` 环境变量 |
| `pnpm install` 报构建脚本被忽略 | 已由 `pnpm-workspace.yaml` 白名单处理（esbuild / vue-demi）；新增依赖触发时按提示审查后加白名单 |
| 端口占用（8080/5173/5174） | 改 `backend/slate-boot/src/main/resources/application.yml` 的 `server.port`，或各 app 的 `vite.config.ts` |
| CI 在哪看 | `.github/workflows/`：后端（编译+测试+ArchUnit+L1 头检查）与前端（lint+type-check+build+头检查）两条防线，PR→dev 时自动跑 |

## 7. 然后去哪

- 提交代码前：读根目录 `AGENTS.md` → [docs/codemap.md](docs/codemap.md) → [docs/product/core.md](docs/product/core.md) → [docs/architecture/iron-laws.md](docs/architecture/iron-laws.md)
- 领任务：[docs/guides/git-workflow.md](docs/guides/git-workflow.md)（分支/PR/Issue 硬性约束）
- 底座设计（当前实现目标）：[docs/design/平台底座/design.md](docs/design/平台底座/design.md)
