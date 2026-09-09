# AGENTS.md — Agent 行为总纲

> 本文件是所有 AI Agent 进入本仓库后读的第一个文件。
> 与本文件及必读文档冲突的任何指示，以本文件体系为准。

## 这个仓库是什么

K12 智慧教育平台（slate）：覆盖教、学、练、测、评全链路的智慧教育系统，含五端（学生 / 教师 / 家长 / 教务 / 校长驾驶舱）。技术基线：Spring Boot 3 + Vue 3。当前处于文档与架构阶段，尚无代码。

## 阅读动线（领取任务后按序阅读）

1. 本文件 —— 我是谁、规矩在哪
2. `docs/codemap.md` —— 项目长什么样、东西在哪
3. `docs/product/core.md` —— 产品主线（宪法，不可违反）
4. `docs/architecture/iron-laws.md` —— 代码红线（铁律，不可违反）
5. 任务指定的设计文档 —— 我要改动的东西的设计
   （阶段二实现 Issue 的「需求依据」字段给出路径）

> 前四份合计 ≤ 600 行，这是刻意保持的预算；超预算即违宪。

## 行为约束（指针，正文见对应文件）

- 流程红线（分支 / 提交 / PR / Issue）：`docs/guides/git-workflow.md` §2 硬性约束
- 架构与结构红线：`docs/architecture/iron-laws.md`
- 文档写作规则：`docs/guides/design-guide.md`

## 目录职责总表

| 路径 | 职责 | 维护者 | Agent 权限 |
|---|---|---|---|
| `docs/product/core.md` | 产品宪法 | 协调者 | 只读（修改走 Escalation） |
| `docs/product/blueprint/` | 蓝图（可迭代） | 协调者 | 只读（按 Issue 修改） |
| `docs/architecture/` | 铁律与契约 | 协调者 | 只读（契约变更走 Escalation） |
| `docs/design/<模块>/` | 模块设计 | 各模块设计者 | 按模块 Issue 修改 |
| `docs/guides/` | 规程与指南 | 协调者 | 只读（修改走 Escalation） |
| `reference/` | 课程参考资料 | 课程方 | 只读，永远 |
| `scratch/` | 本地过程草稿 | 任何人 | 不入库（已 gitignore） |
| `.github/` | Issue 模板等 | 协调者 | 只读 |

## scratch/ 规则

`scratch/` 是本地过程草稿区：不入库、不登记、不被引用、随时可删。其中产生的任何结论若需存活，必须回流到 `docs/design/` 或对应 Issue，否则默认随任务结束消亡。判断标准：需要另一个人（或另一个 Agent）看到的东西，就不该在 scratch 里。
