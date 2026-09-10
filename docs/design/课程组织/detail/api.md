# 课程组织 对外契约（detail/api.md）

status: draft
所属域: 课程　维护者: agent-codex　父文档: [../design.md](../design.md)

> 前缀 `/api/v1/courses`（[api-conventions §1](../../../architecture/contracts/api-conventions.md) 资源前缀表已登记 `courses` → 课程，**本包不新增前缀**）。
> 实体与字段见 [data.md](data.md)；错误码 `CRS-*` 为首次定义，一经合并按 [api-conventions §4](../../../architecture/contracts/api-conventions.md) 语义冻结、只增不改。

## 1. 端点清单

### 1.1 课程档案

| 方法与路径 | 语义 | 权限 |
|---|---|---|
| GET `/courses` | 课程分页查询（过滤 `stageCode` / `subjectCode` / `editionId` / `status` / `keyword`） | `course:course:read` |
| GET `/courses/{id}` | 课程详情（返回三维归属 + 教材版本摘要） | `course:course:read` |
| POST `/courses` | **建课**：按三维创建，落 `DRAFT` | `course:course:write` |
| PATCH `/courses/{id}` | **改课**：部分更新，字段可变性见 [data.md §4.1](data.md) | `course:course:write` |
| POST `/courses/{id}/submit` | 提交审核：`DRAFT` → `PENDING_REVIEW` | `course:course:write` |
| POST `/courses/{id}/approve` | 审核通过（上架）：`PENDING_REVIEW` → `PUBLISHED` | `course:course:review` |
| POST `/courses/{id}/reject` | 审核驳回：`PENDING_REVIEW` → `DRAFT` | `course:course:review` |
| POST `/courses/{id}/disable` | **停用**：→ `DISABLED`（终态，幂等） | `course:course:write` |

无 DELETE 端点：停用即终态，数据不物理删除（[../design.md](../design.md) §3.3）。

### 1.2 三维字典维护

| 方法与路径 | 语义 | 权限 |
|---|---|---|
| GET `/courses/textbook-editions` | 教材版本分页查询（过滤 `stageCode` / `subjectCode` / `status` / `keyword`） | `course:edition:read` |
| POST `/courses/textbook-editions` | 新建教材版本 | `course:edition:write` |
| PATCH `/courses/textbook-editions/{id}` | 改教材版本（**仅** `status` / `remark`，身份字段不可变） | `course:edition:write` |
| POST `/courses/textbook-editions/{id}/disable` | 停用教材版本 | `course:edition:write` |
| DELETE `/courses/textbook-editions/{id}` | 删除教材版本（**仅未被任何课程引用时**，否则 `CRS-009`） | `course:edition:write` |
| GET `/courses/subject-stage-scopes` | 学科-学段适用关系查询（过滤 `stageCode` / `subjectCode` / `enabled`） | `course:scope:read` |
| POST `/courses/subject-stage-scopes` | 新增适用关系（「某学科在某学段开设」） | `course:scope:write` |
| PATCH `/courses/subject-stage-scopes/{id}` | 启用 / 停用适用关系 | `course:scope:write` |
| DELETE `/courses/subject-stage-scopes/{id}` | 删除适用关系（**仅无课程使用该组合时**，否则 `CRS-012`） | `course:scope:write` |

> 学段与学科两个维度**不在本前缀下**：其字典维护走底座 `sys:dict:*` 权限与 `GET/POST /sys/dicts`（[系统管理契约](../../平台底座/detail/api/系统管理.md) §1）。本域只消费。

## 2. 建课与改课的校验顺序

一次写入可能同时命中多个问题，按 [data.md §3](data.md) 的固定顺序校验、返回**首个**失败码，保证错误可复现：

```
CRS-003 字典项无效 → CRS-002 组合不受支持 → CRS-004 教材版本不可用
→ CRS-005 教材版本与三维不一致 → CRS-001 同三维同名 → CRS-007 上架后改三维
→ CRS-006 状态不允许该操作
```

改课（PATCH）只对**请求中实际出现的字段**做校验：未提交三维则跳过 `CRS-003/002/004/005/007`。

## 3. 分页、过滤与响应

- 分页参数 `page`（1 起，默认 1）、`size`（默认 20，上限 100）、`sort`（`field,desc`，可多组），响应 `data` 固定 `{ list, total, page, size }`（[api-conventions §3/§5](../../../architecture/contracts/api-conventions.md)）
- 过滤参数平铺在 query 上；时间范围用 `createdAtFrom` / `createdAtTo`
- 统一响应 `{ code, message, data, traceId }`，`traceId` 必返；传输层错误用 HTTP 状态码（如读取不存在的课程返回 404）
- 字段 camelCase；时间 `yyyy-MM-dd'T'HH:mm:ss`（北京时间）
- 课程列表项返回 `stageCode` / `subjectCode` 的 **code**，展示名由前端按底座字典映射（[data.md §2.1](data.md)）

## 4. Agent-ready 三件套挂接（铁律 L8）

三件套的机制由底座提供，业务代码声明式接入（[底座设计 §3.4](../../平台底座/design.md)）：幂等 = `IdempotentFilter`（写请求 + `Idempotency-Key` 头，Redis 缓存首次结果 24h，业务零接入）；审计 = `@Audited` 注解切面；事件 = `DomainEventPublisher`（outbox 模式）。

**全部写操作逐条挂接**：

| # | 写操作 | 幂等 | 审计（`@Audited` 动作） | outbox 事件 |
|---|---|---|---|---|
| 1 | POST `/courses` 建课 | `Idempotency-Key` | `course:create` | `course.course.created` |
| 2 | PATCH `/courses/{id}` 改课 | `Idempotency-Key` | `course:update` | `course.course.updated` |
| 3 | POST `/courses/{id}/submit` 提交审核 | `Idempotency-Key` | `course:submit` | `course.course.status-changed` |
| 4 | POST `/courses/{id}/approve` 审核上架 | `Idempotency-Key` | `course:approve` | `course.course.status-changed` |
| 5 | POST `/courses/{id}/reject` 审核驳回 | `Idempotency-Key` | `course:reject` | `course.course.status-changed` |
| 6 | POST `/courses/{id}/disable` 停用课程 | `Idempotency-Key` | `course:disable` | `course.course.status-changed` |
| 7 | POST `/courses/textbook-editions` 新建版本 | `Idempotency-Key` | `course:edition-create` | `course.edition.created` |
| 8 | PATCH `/courses/textbook-editions/{id}` 改版本 | `Idempotency-Key` | `course:edition-update` | `course.edition.updated` |
| 9 | POST `/courses/textbook-editions/{id}/disable` 停用版本 | `Idempotency-Key` | `course:edition-disable` | `course.edition.updated` |
| 10 | DELETE `/courses/textbook-editions/{id}` 删版本 | `Idempotency-Key` | `course:edition-delete` | `course.edition.deleted` |
| 11 | POST `/courses/subject-stage-scopes` 新增适用关系 | `Idempotency-Key` | `course:scope-create` | `course.scope.changed` |
| 12 | PATCH `/courses/subject-stage-scopes/{id}` 启停关系 | `Idempotency-Key` | `course:scope-update` | `course.scope.changed` |
| 13 | DELETE `/courses/subject-stage-scopes/{id}` 删关系 | `Idempotency-Key` | `course:scope-delete` | `course.scope.changed` |

约定：

- 事件名沿用底座既有形态 `<域>.<聚合>.<事件>`（对照 [组织架构契约](../../平台底座/detail/api/组织架构.md) 的 `org.class.member-changed`）
- 事件载荷（ID + 变更摘要）以 [agent-boundary](../../../architecture/contracts/agent-boundary.md) 契约为准，本设计只登记**事件名与触发点**，不重复定义载荷
- 审计经 `@Audited` 记录双身份（`X-Service-Id` + `X-On-Behalf-Of`）；未来 Agent 经本契约写入时同样留痕（铁律 L8）
- 停用课程事件是下游的唯一通知手段——本域不反向调用下游（[../design.md](../design.md) §3.4）

## 5. 错误码（CRS）

| 码 | 语义 | 触发点 |
|---|---|---|
| `CRS-001` | 同一三维（学段×学科×教材版本）下课程名已存在 | 建课、改课 |
| `CRS-002` | 学段与学科的组合不受支持（无适用关系或该关系已停用） | 建课、改三维 |
| `CRS-003` | 学段或学科不是有效字典项 | 建课、改三维、适用关系维护 |
| `CRS-004` | 教材版本不存在或已停用 | 建课、改课 |
| `CRS-005` | 教材版本与课程的学段/学科不一致 | 建课、改三维 |
| `CRS-006` | 课程当前状态不允许该操作 | 改课、状态流转、适用关系删除 |
| `CRS-007` | 课程已上架，三维归属不可变更 | 改三维 |
| `CRS-008` | 教材版本重复（同学段×学科×出版社×版本名×修订年） | 新建教材版本 |
| `CRS-009` | 教材版本已被课程引用，不可删除 | 删除教材版本 |
| `CRS-010` | 课程不存在（外部传入的 `courseId` 无效） | 被引用方查询、跨域引用校验 |
| `CRS-011` | 学科-学段适用关系重复 | 新增适用关系 |
| `CRS-012` | 适用关系已被课程使用，不可删除 | 删除适用关系 |
| `CRS-013` | 并发修改冲突（乐观锁版本不一致，请重读后重试） | 改课 |

- `CRS-010` 用于**外部传入的引用无效**（对照 `ORG-005`）；本域自身读路径的资源缺失走 HTTP 404
- `message` 面向开发者；面向教师/学生的文案由前端按 code 映射（[api-conventions §4](../../../architecture/contracts/api-conventions.md)）
- 框架级兜底错误（参数校验失败、方法不支持、内部错误）沿用 `SYS-001/002/999`，本域不重复定义

## 6. 变更记录

| 版本 | 日期 | 变更 | 依据 |
|---|---|---|---|
| v0.1 | 2026-09-10 | 成文：16 端点、校验顺序、分页与响应、三件套逐条挂接、CRS-001~013 错误码 | Issue #7 阶段一设计 |
