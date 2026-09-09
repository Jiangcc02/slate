# iron-laws — 代码铁律

> 任何代码变更不得违反本文件，违反 = PR 直接打回，无讨论空间。
> 与 `guides/git-workflow.md` §2 的分工：那份管流程红线，本文件管代码与结构红线。
> 标注 TODO-coordinator 的条款由协调者在底座开发时填充，填充前按现有文字执行。

## L1 文件头注释（每个代码文件必须）

```
// 域/模块: 测评/错题本
// 类型: Service 实现
// 职责: 错题自动归组与复习计划生成
// 设计文档: docs/design/错题本/design.md
// 维护者: 张三 / agent-claude
```

- 「职责」行必须让文件脱离任何文档也能被理解（自描述是第一道防线）
- 「设计文档」行是溯源信息（出生证明），不是运行依赖；文件职责变化时才更新
- Java/TS/Vue 用对应注释语法，SQL 用 `--`，结构相同

## L2 目录与 AGENTS.md

- 新建源码目录必须同 PR 创建该目录的 AGENTS.md（职责、维护者、规则）
- 文件放对位置：以 `docs/codemap.md` 的地图为准；无处可放 = 停下询问，不自创顶层目录

## L3 只读区

以下内容不得修改（Issue 明确要求或人类指示除外，否则走 Escalation）：
`reference/`、`docs/product/core.md`、`docs/architecture/`（含本文件与契约）、`docs/guides/`、`.github/`

## L4 契约至上

- API 风格、错误模型、认证方式遵循 `contracts/api-conventions.md`，不得自创
- 数据所有权以 `contracts/data-ownership.md` 为准；跨域取数走契约接口，禁止直连他域表

## L5 文档同步分档

代码改动是否同步设计文档，按「重实现测试」分档（细则见 `guides/design-guide.md` §5）：
拿着现有设计文档重新实现，会得到与改后一致的代码 → 文档不用动；不一致 → 必须同 PR 更新。
实现期的文档改动限于「中档」以内；触碰结构与契约（大档）= 停下报告设计者。

## L6 产物与敏感信息

- 构建产物、依赖包、IDE 配置不入库（见 `.gitignore`；出现新类型须同 PR 补充）
- 密钥、口令、token 永不入库；一旦疑似入库：第一动作作废并轮换，第二动作清理历史（需 force push，走 Escalation）

## L7 架构占位（TODO-coordinator，底座开发时填充）

- 分层边界与调用方向：
- 域间依赖方向（业务域之间是否允许互相依赖、经什么形式）：
- 平台底座与公共组件的修改权规则：
