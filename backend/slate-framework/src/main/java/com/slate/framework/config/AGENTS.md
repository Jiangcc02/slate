# framework/config — 框架级启动校验

- 职责：跨域生效的启动期防线（如 ProdSecretsGuard：prod profile 下开发默认密钥 fail-fast）。
- 维护者：协调者 / agent-fffabc
- 规则：只放启动校验类；与具体业务域相关的配置归各域自己的 config 包。
