# 平台底座 设计文档

status: active
所属域: 平台底座
维护者: 协调者侧（交付署名 agent-fffabc）
关联 Issue: 无（协调者侧骨架工作，不走任务包；本文档为底座实现任务包的需求依据）

## 1. 这个模块解决什么问题

底座是九域共用的地基（蓝图：底座必须先行，后续所有包依赖它）。一期目标 = 蓝图「成功画面」：任一业务域开工时，登录态、权限、统一响应、异常、幂等、审计、事件外发全部现成可用，设计者只按契约实现业务。本阶段交付物验收标准是 Agent-ready 的环境（core §1），不含 Agent。

## 2. 边界

- 对外提供：
  - 六个能力模块的契约 API（前缀见 [api-conventions §1](../../architecture/contracts/api-conventions.md)）：`auth` / `users` / `roles`（认证授权与用户）、`orgs`（组织架构）、`messages`（消息中心）、`files`（文件服务）、`sys`（系统管理）
  - 工程规范机制（slate-framework）：统一响应与异常、traceId 链路、雪花 ID、幂等、操作审计、领域事件外发
- 依赖别人：无业务域依赖（被全部业务域依赖）；基础设施依赖 MySQL 8 / Redis / MinIO（S3 协议）
- 明确不做：任何业务逻辑；教室设备管理（四期）；Agent 运行时（建设次序后置）； WebSocket 实时推送（一期站内信轮询起步，随堂测等场景三期再引入）

## 3. 概要设计

### 3.1 七项关键决策（协调者裁定 2026-09-09，前两项回填契约）

| # | 决策点 | 定稿 | 依据 |
|---|---|---|---|
| 1 | 主链 ID 生成 | **雪花 ID**（int64：41 位毫秒时间戳 + 10 位 workerId + 12 位序列，epoch=2025-01-01，workerId 0~1023 全系统唯一配置） | 趋势递增、不暴露业务量级、免自增协调；[global-data-model §2](../../architecture/contracts/global-data-model.md) 回填 |
| 2 | JWT 过期与刷新 | **双 token**：access 2h（接口鉴权）+ refresh 7d 滚动刷新（续期即换新） | 教室端长会话靠 refresh 支撑、盗用窗口小；[api-conventions §2](../../architecture/contracts/api-conventions.md) 回填 |
| 3 | 幂等 TTL | **24 小时**（Redis，同 `Idempotency-Key` 返回首次结果） | 覆盖隔日重试；[api-conventions §6](../../architecture/contracts/api-conventions.md) 回填 |
| 4 | RBAC 模型 | 账号-角色-权限三级；权限粒度到菜单/按钮级（core §2）；预置六角色：学生/教师/班主任/教务/管理员/校长（校长端=只读总览角色） | core §2、蓝图 |
| 5 | 操作审计 | 注解式切面（`@Audited`）：记录 谁（账号+末端用户）/何时/何端/动作/目标/参数摘要/结果；写操作强制、敏感查询（成绩类）显式标注；日志只追加 | 铁律 L8、api-conventions 双身份头 |
| 6 | 领域事件外发 | **本地事件表（outbox）**：业务事务内写 event 表，独立投递器轮询外发（HTTP 回调起步），失败退避重试；事件契约见 [agent-boundary](../../architecture/contracts/agent-boundary.md) | Agent-ready 义务（L8）：可观测；避免双写不一致 |
| 7 | 文件存储 | MinIO（S3 协议）起步，云 OSS 可替换（存储层抽象）；元数据入库，实体归底座 | 蓝图技术底座 |

### 3.2 工程骨架（已落地，backend/）

Maven 多 module 渐进式：`slate-common`（契约模型，无 Spring）← `slate-framework`（本设计 §3.1 之 3/4/5/6 机制所在）← `slate-platform`（六个能力模块实现，`api`/`internal` 包分离）← `slate-boot`（唯一启动器）。结构详见 [backend/AGENTS.md](../../../backend/AGENTS.md)；ArchUnit 在 CI 强制 L7（模块单向、internal 封闭、分层单向、无环）。

### 3.3 六个能力模块概要

| 模块 | 核心内容 | 关键点 |
|---|---|---|
| 认证授权 | 登录/登出/刷新、JWT 签发校验、RBAC 管理、登录日志 | 密码 BCrypt；连续失败锁定；token 撤销靠 refresh 白名单（Redis） |
| 组织架构 | 机构→校区→学部→年级→班级五级树、班级学生名单、教师任课关系 | 树用物化路径（path）；名单与任课关系是全系统主链事实（data-ownership） |
| 用户中心 | 五类角色档案、批量导入（Excel）、账号生命周期、家长-学生多对多绑定 | 绑定须家长确认（合规：监护人同意）；敏感字段加密存储、展示脱敏（core §7） |
| 消息中心 | 站内信、公告、通知渠道（邮件/短信/微信预留接口） | 投递通道归底座，定向触达业务规则归家校域（data-ownership 仲裁） |
| 文件服务 | 上传/下载/预览签名 URL、元数据管理、配额与类型白名单 | 课件/视频/图片；直传 MinIO 预签名 URL，不占业务带宽 |
| 系统管理 | 数据字典、参数配置、登录/操作日志查询、 Knife4j 接口文档聚合 | 字典/参数供全系统消费；日志只读查询 |

### 3.4 Agent-ready 三件套的落地方式（L8 义务）

1. **幂等**：framework 提供 `@Idempotent` 拦截器（Redis SETNX + 首次结果缓存 24h），业务接口声明式接入；Agent 双身份调用自动受益
2. **审计**：`@Audited` 切面 + MDC traceId，所有写操作默认进操作日志（含 `X-Service-Id` / `X-On-Behalf-Of` 双身份）
3. **事件外发**：`DomainEventPublisher`（事务提交后落 outbox），业务域声明事件类型外发；Agent 运行时未来订阅

## 4. 详细设计（detail/）

- 数据模型：[detail/data.md](detail/data.md)（底座全部实体 ER + Redis 键设计）
- 对外契约：[detail/api/](detail/api/) 按模块拆分（认证授权 / 组织架构 / 用户中心 / 消息中心 / 文件服务 / 系统管理）

## 5. 验收对照

| 验收来源 | 条目 | 本设计覆盖 |
|---|---|---|
| 蓝图模块清单（一期 7 项） | 认证授权 / 组织架构 / 用户中心 / 消息中心 / 文件服务 / 系统管理 / 工程规范 | §3.3 六模块 + §3.2/§3.4 工程规范与三件套 |
| 蓝图成功画面 | 业务域开工时地基现成可用 | §2 对外提供清单 |
| core §7 质量底线 | 冒烟可验、AI 留痕（审计）、未成年人加密脱敏 | §3.1 决策 5、§3.3 用户中心行 |
| core §1 / L8 Agent-ready | 事件外发、契约 API、幂等、审计 | §3.4 三件套 |
| 契约悬置回填 | ID / JWT / 幂等 TTL | §3.1 决策 1-3，随本 PR 回填契约并冻结 api-conventions v1.0 |

## 6. 变更记录

| 版本 | 日期 | 变更 | 依据 |
|---|---|---|---|
| v1.0 | 2026-09-09 | 成文：七项决策定稿、六模块概要、Agent-ready 三件套、detail 全量 | 协调者七项裁定（2026-09-09） |
