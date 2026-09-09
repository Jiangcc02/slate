# backend/ — 后端工程（Spring Boot 3 模块化单体）

> 遵守根 `AGENTS.md` 与 `docs/architecture/iron-laws.md`；本文件只写本目录特有规则。

## 结构（渐进式，业务域 module 随任务包添加）

| module | 职责 | 依赖 |
|---|---|---|
| `slate-common` | 统一响应 / 错误码契约 / 分页对象，无 Spring 依赖 | 无 |
| `slate-framework` | 工程规范机制：全局异常、traceId 链路、雪花 ID；后续幂等 / 审计 / 事件外发 | common |
| `slate-platform` | 平台底座域（认证授权 / 组织架构 / 用户中心 / 消息 / 文件 / 系统管理） | common, framework |
| `slate-boot` | 唯一启动器与全局配置，聚合所有 module | platform（传递全部） |

## 本目录规则

- **module 添加**：新业务域 module 随其任务包创建，同 PR 登记本表并配 AGENTS.md；不预建空壳
- **包结构约定**：域 module 内对外契约放 `com.slate.<module>.api` 包，实现放 `internal` 包——跨模块只许 import 别人的 `api` 包（铁律 L7，ArchUnit 在 slate-boot 测试中强制）
- **启动入口唯一**：只有 `slate-boot` 有 main 与 repackage，其他 module 是库
- **版本治理**：依赖版本只在父 POM `dependencyManagement` 声明，子 POM 不写版本号
- 每个提交进来的 `.java` 文件必须带 L1 文件头（CI 脚本 `scripts/check-file-headers.sh` 强制）

## 维护者

协调者侧（平台底座领地，见 `docs/product/blueprint/平台底座.md`）——交付署名 agent-fffabc
