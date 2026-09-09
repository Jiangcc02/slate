# API 约定

status: v0.9-draft（内容就绪，随底座首个任务包 PR 验证后冻结 v1；冻结后变更须协调者裁定并在所有 open Issue 广播）

> 本文件定义全局 API 风格、认证方式、错误模型。所有域与模块的对外接口必须遵循（铁律 L4）。

## 1. 路径与命名

- 风格：RESTful，资源名词复数、kebab-case，统一前缀 `/api/v1`，例：`/api/v1/courses/{courseId}/chapters`
- 资源前缀（路径第一段）与属主域对照，新增前缀须登记本表：

| 前缀 | 属主域 |
|---|---|
| `auth` / `users` / `roles` | 平台底座（认证 / 用户 / 权限） |
| `orgs`（组织架构、班级、任课关系） | 平台底座 |
| `messages` / `files` / `sys` | 平台底座（消息 / 文件 / 系统管理） |
| `recruitments` | 招生 |
| `courses`（章 / 节 / 课时 / 课件 / 课堂记录） | 课程 |
| `learnings` / `assignments`（作业任务流） | 学习 |
| `exams`（题库 / 组卷 / 三形态 / 答题 / 成绩） | 测评 |
| `portraits`（学情画像 / 成长档案 / 预警） | 学情 |
| `schedules`（校历课表 / 考务 / 学籍） | 教务 |
| `notices` / `leave-requests` / `communications` | 家校协同 |
| `bills` | 收费运营 |

- 动作用 HTTP 方法表达：GET 查询 / POST 创建 / PUT 全量更新 / PATCH 部分更新 / DELETE 删除；非 CRUD 动作用子资源，例：`POST /api/v1/assignments/{id}/submissions`（提交作业）
- JSON 字段 camelCase；时间一律 `yyyy-MM-dd'T'HH:mm:ss`，北京时间（UTC+8）

## 2. 认证与鉴权

- 登录态：JWT Bearer——请求头 `Authorization: Bearer <token>`（Spring Security 签发）
- token claims：用户 ID、角色集、组织 ID、过期时间；过期与刷新策略由底座设计定稿
- 权限模型：RBAC 到菜单/按钮级（core §2），接口层注解鉴权
- 服务间调用（含未来 Agent 运行时）：双身份头 `X-Service-Id`（服务身份）+ `X-On-Behalf-Of`（末端用户），业务侧按两者叠加鉴权与审计（对齐 `agent-boundary.md`）

## 3. 统一响应结构

```json
{ "code": 0, "message": "ok", "data": {}, "traceId": "3f2a…" }
```

- `code`：0 = 成功，非 0 = 业务错误码（§4）；传输层错误用 HTTP 状态码表达
- `traceId`：与日志链路一致，必返（蓝图 §十三 生产工程）
- 分页响应的 `data` 固定为：`{ "list": [], "total": 123, "page": 1, "size": 20 }`

## 4. 错误码体系

- 格式：`<域码>-<三位序号>`，字符串（协调者裁定 2026-09-09），例：`AUTH-001`
- 域码表：AUTH 认证 / ORG 组织架构 / USER 用户 / MSG 消息 / FILE 文件 / SYS 系统管理（底座）；RCRT 招生 / CRS 课程 / LRN 学习 / ASM 作业 / EXM 测评 / POR 学情 / SCH 教务 / HSC 家校 / BILL 收费
- 错误码一经发布语义冻结，只增不改；`message` 面向开发者，面向学生的文案由前端按 code 映射

## 5. 分页、排序、过滤

- 请求参数：`page`（1 起，默认 1）、`size`（默认 20，上限 100）、`sort`（`field,desc`，可多组）
- 过滤参数平铺在 query 上；时间范围用 `fieldFrom` / `fieldTo`
- 超大列表（如全量题库导出）不走分页，走异步导出任务

## 6. 幂等与写操作（Agent-ready，铁律 L8）

- 所有写操作（POST / PATCH / DELETE）支持 `Idempotency-Key` 请求头：同 key 重复请求返回首次结果
- 底座提供统一幂等实现（Redis），业务代码声明式接入；TTL 由底座设计定稿

## 7. 接口版本策略

- URL 版本：`/api/v1`；破坏性变更升 `v2`，兼容期内双版本并行并公告下线时间
- 新增字段、新增端点不算破坏性变更，直接迭代

## 变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| v0-draft | 2026-09-09 | 占位建立 |
| v0.9-draft | 2026-09-09 | 七节内容成文（路径 / 认证 / 响应 / 错误码 / 分页 / 幂等 / 版本），错误码风格裁定为域前缀字符串；待底座首个任务包验证后冻结 v1 |
