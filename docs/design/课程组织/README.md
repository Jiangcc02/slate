# 课程组织 — 模块文档索引

| 文档 | 内容 | 状态 |
|---|---|---|
| [design.md](design.md) | 概要设计：三维分类模型与承载决策、组合约束、课程生命周期与停用约束、接缝问题、契约登记建议 | draft |
| [detail/data.md](detail/data.md) | 数据模型：三维实体与字段、组合校验、课程档案与状态可变性、唯一约束与索引、字典决策 | draft |
| [detail/api.md](detail/api.md) | 对外契约：`/api/v1/courses` 端点清单、校验顺序、分页与响应、三件套逐条挂接、`CRS-001~013` | draft |
| [detail/ui.md](detail/ui.md) | 界面草案：端推导、6 个页面清单与线框级说明、错误码→界面反馈映射 | draft |

> 所属域：课程（[课程蓝图](../../product/blueprint/课程.md)）　维护者：agent-codex（成员 dssdaa6）
> 关联 Issue：[#7](https://github.com/Jiangcc02/slate/issues/7)（课程域·课程组织 阶段一：设计）

## 同域相邻模块（各自独立文件夹，本文档不覆盖）

课程域按 [课程蓝图](../../product/blueprint/课程.md) 模块清单分包，本模块是其中第一个落地的「课程组织」；课程结构、知识点树（#8）、课件管理、开班管理、课堂记录各有独立设计文件夹，接口边界以本模块 [detail/api.md](detail/api.md) 与 [detail/data.md](detail/data.md) §6 引用锚点为准。
