# codemap — 项目地图

> 给人或 Agent 的 30 秒入口：项目是什么、东西在哪、该去读什么。

## 项目一句话

面向 K12 的智慧教育平台：教、学、练、测、评全链路闭环，六端（学生 / 教师 / 家长 / 教室端 / 教务 / 校长端），9 个业务域 + 平台底座 + Agent 运行时。技术基线：Spring Boot 3（业务单体）+ Python（Agent 运行时）+ Vue 3。

## 目录地图

| 路径 | 是什么 | 详细导航 |
|---|---|---|
| `backend/` | 后端工程：Spring Boot 3 模块化单体（Maven 多 module，结构见 `backend/AGENTS.md`） | `backend/AGENTS.md` |
| `frontend/` | 前端工程：Vue 3 pnpm monorepo（管理端/学生端 + 教室端占位 + shared 共享包） | `frontend/AGENTS.md` |
| `docs/product/core.md` | 产品宪法：定位、角色、域清单、契约原则 | — |
| `docs/product/blueprint/` | 各域蓝图与规划 | `blueprint/README.md` |
| `docs/architecture/iron-laws.md` | 代码不可违反的铁律 | — |
| `docs/architecture/contracts/` | API 约定、数据所有权、Agent 边界、全局数据模型（主链骨架） | 各文件头部 status |
| `docs/design/<模块>/` | 模块设计文档（设计者领地） | 各模块 `README.md` |
| `docs/guides/git-workflow.md` | 分支 / PR / Issue 流程规程 | — |
| `docs/guides/design-guide.md` | 设计文档怎么写、怎么评审 | — |
| `reference/` | 课程参考资料（只读） | `reference/README.md` |

## 我该读什么（按角色）

- **新进的 Code Agent**：根 `AGENTS.md` → 本文件 → core → iron-laws → 任务指定的设计文档
- **模块设计者**：`guides/design-guide.md`（写作规则与评审标准）+ `guides/git-workflow.md`（流程）
- **要了解某模块现状**：`docs/design/<模块>/README.md` → `design.md` → `detail/`
- **要了解某域规划**：`docs/product/blueprint/<域>.md`

## 查找动线（≤ 3 跳定位任何产物，禁止全量加载）

1. **先查契约路由表**（O(1)，只看表不读正文）：
   - 按接口找：`contracts/api-conventions.md` §1 资源前缀表——`/api/v1/exams/…` → 测评域
   - 按数据找：`contracts/data-ownership.md` 清单——「成绩」→ 测评域
2. **固定路径推导**：`docs/design/<域>/detail/api.md`（对外契约）/ `detail/data.md`（实体 ER）——命名固定，不用搜索
3. **索引兜底**：目标域 `README.md`（detail 已拆目录时，一行一个指到子文件）

grep 是最后手段（命名与前缀固定保证可 grep）。**任务内不需要查找**——阶段二 Issue「需求依据」直接给出路径，查找只用于跨域引用。必读上下文 = 顶层四份（≤600 行预算）+ 定位后按需加载的 1-2 个 detail 文件。

## 当前状态（2026-09）

- 仓库进入代码阶段起点（2026-09-09）：`backend/` 工程骨架 + CI 防线已落（Spring Boot 3 / JDK 17 / Maven 4 module 渐进式，业务域 module 随任务包添加）
- 蓝图已分域迁移（2026-09-09）：总纲 `K12教育系统.md`（跨域角色/流程/闭环/架构决策）+ `blueprint/` 一域一文件（含教室端专题）
- 契约层：api-conventions **v1.0 冻结** / data-ownership v1.0 / 主链骨架（ID=雪花已定稿）/ Agent 边界——三项悬置参数经底座设计定稿回填（2026-09-09）
- 双运行时架构已定（2026-09）：Spring Boot 业务单体 = 数字学校本体，Python Agent 运行时 = 大模型的运行与操作环境，边界契约 `contracts/agent-boundary.md`
- 建设次序：当前阶段只建业务单体（验收标准 Agent-ready），Agent 运行时开发在系统生产稳定运行后启动（core.md §1）
