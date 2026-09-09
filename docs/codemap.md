# codemap — 项目地图

> 给人或 Agent 的 30 秒入口：项目是什么、东西在哪、该去读什么。

## 项目一句话

面向 K12 的智慧教育平台：教、学、练、测、评全链路闭环，五端（学生 / 教师 / 家长 / 教务 / 校长驾驶舱），8 个业务域 + 平台底座。技术基线 Spring Boot 3 + Vue 3。

## 目录地图

| 路径 | 是什么 | 详细导航 |
|---|---|---|
| `docs/product/core.md` | 产品宪法：定位、角色、域清单、契约原则 | — |
| `docs/product/blueprint/` | 各域蓝图与规划 | `blueprint/README.md` |
| `docs/architecture/iron-laws.md` | 代码不可违反的铁律 | — |
| `docs/architecture/contracts/` | API 约定、数据所有权（v0-draft） | 各文件头部 status |
| `docs/design/<模块>/` | 模块设计文档（设计者领地） | 各模块 `README.md` |
| `docs/guides/git-workflow.md` | 分支 / PR / Issue 流程规程 | — |
| `docs/guides/design-guide.md` | 设计文档怎么写、怎么评审 | — |
| `reference/` | 课程参考资料（只读） | `reference/README.md` |

## 我该读什么（按角色）

- **新进的 Code Agent**：根 `AGENTS.md` → 本文件 → core → iron-laws → 任务指定的设计文档
- **模块设计者**：`guides/design-guide.md`（写作规则与评审标准）+ `guides/git-workflow.md`（流程）
- **要了解某模块现状**：`docs/design/<模块>/README.md` → `design.md` → `detail/`
- **要了解某域规划**：`docs/product/blueprint/<域>.md`

## 当前状态（2026-09）

- 仓库处于文档与架构阶段，尚无代码
- 现有蓝图内容暂集中在 `docs/product/K12教育系统.md`，将逐步迁移至 `blueprint/` 分域文件
- 契约层为 v0-draft 占位，待底座开发产出
