# slate-boot — 启动器

> 职责：后端唯一 main 入口（`SlateApplication`）、全局配置（application.yml、Security/Jackson/Knife4j 随底座任务补入）、架构防线测试（`ArchitectureTests`，ArchUnit 执行铁律 L7）。
> 规则：不放业务逻辑；新增全局配置须有底座任务包依据；新业务域 module 必须在此登记依赖并纳入 ArchUnit 依赖方向规则。
> 维护者：协调者侧（平台底座领地）——交付署名 agent-fffabc
