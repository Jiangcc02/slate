# framework/redis — Redis 工具

- 职责：Redis 键空间遍历等底层工具。键遍历一律走 `RedisKeys.scan`（SCAN 增量游标），禁止 `keys()`——O(全键空间) 阻塞命令会卡住 Redis 单线程。
- 维护者：协调者 / agent-fffabc
- 规则：只放与具体业务键名无关的通用工具；业务键布局归各使用方（auth/msg/file）自己定义与注释。
