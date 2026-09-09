# 技术文档配套代码包（resources/）

> 本目录是《软件项目开发综合实验》16 篇学生技术文档（见 `../P01-*.md` ~ `../P16-*.md`）的
> **配套可运行代码包**。按「一次课一个目录」组织，与教案、学生文档一一对应。
> 所有代码以「校园二手书交易平台（bookshop）」为贯穿案例：包名 `com.example.bookshop`、
> 数据库 `bookshop`、后端 8080 / 前端 5173。

---

## 1. 与 `../文档模板/` 的关系（必读）

本目录只放 **代码 + 不可迁移的二进制资源**， **Markdown 学生提交模板已统一迁出**到
`../文档模板/`（15 个 T01–T15 模板 + README 索引）。任何 md 模板相关问题，先查
`../文档模板/README.md`。

| 你要的东西                                    | 在哪儿                                                                      |
|-----------------------------------------------|-----------------------------------------------------------------------------|
| 学生需要填写的 Markdown 交付物模板（T01–T15） | **`../文档模板/`**                                                          |
| 可运行 Spring Boot / MyBatis-Plus 后端工程    | `resources/P02/P04/P06/P07/P08/P11/P14`                                     |
| Vue3 + Vite 前端工程                          | `resources/P05/P06/P12`                                                     |
| E-R 图（SVG）                                 | `resources/P01-bookshop/P01-er-diagram.svg`                                 |
| E-R 图模板（drawio，可编辑）                  | `resources/P10-db-design/templates/er-diagram-template.drawio`              |
| Postman 接口集合                              | `resources/P11-backend-arch/postman/`、`resources/P13-integration/postman/` |
| 答辩 PPT 母版（pptx）                         | `resources/P15-uat/templates/ppt-template.pptx`                             |
| 各目录说明（README）                          | 每个目录内的 `README.md`                                                    |

---

## 2. 目录总览（按课次）

> 16 个目录与 `../P01-*.md` ~ `../P16-*.md` 一一对应；模板类见上节。

| #  | 目录                     | 对应课次                 | 形态     | 关键内容                                                                | 关键技术                |
|----|--------------------------|--------------------------|----------|-------------------------------------------------------------------------|-------------------------|
| 01 | `P01-bookshop/`          | P01 项目立项与数据库设计 | 资源     | `bookshop.sql` + `P01-er-diagram.svg`                                   | MySQL DDL、E-R 建模     |
| 02 | `P02-bookshop-dao/`      | P02 MyBatis 数据访问层   | 后端工程 | `pom.xml` + `src/`                                                      | MyBatis、驼峰映射       |
| 03 | `P03-bookshop-service/`  | P03 业务抽象与接口设计   | 半代码   | `service/` + `docs/` UML                                                | 分层架构、SOLID         |
| 04 | `P04-bookshop-tx/`       | P04 业务实现与事务管理   | 半代码   | `service/` + `exception/` + `test/`                                     | `@Transactional`、AOP   |
| 05 | `P05-bookshop-frontend/` | P05 Vue3 组件化          | 前端工程 | `package.json` + `src/`                                                 | Vue3、Pinia、Router     |
| 06 | `P06-frontend-backend/`  | P06 前后端联调与跨域     | 双工程   | `frontend/` + `backend/`                                                | CORS、axios、Token      |
| 07 | `P07-perf-basics/`       | P07 整合优化             | 半代码   | `config/` + `mapper/` + `controller/`                                   | MyBatis-Plus、StopWatch |
| 08 | `P08-redis-interceptor/` | P08 Redis 与拦截器       | 后端工程 | `config/` + `service/` + `interceptor/`                                 | 缓存、鉴权              |
| 09 | `P09-proposal/`          | P09 项目立项与需求分析   | 范例     | `examples/bookshop-proposal-example.md`                                 | 需求工程                |
| 10 | `P10-db-design/`         | P10 数据库设计与实现     | 资源     | `templates/er-diagram-template.drawio` + `examples/secondhand-book.sql` | 数据库设计              |
| 11 | `P11-backend-arch/`      | P11 后端架构与 API       | 后端工程 | `controller/` + `mapper/` + `postman/` + `docs/swagger-guide.md`        | REST、Swagger           |
| 12 | `P12-frontend-pages/`    | P12 前端页面开发         | 前端工程 | `views/` + `components/` + `stores/` + `api/` + `router/`               | Vue Router、组件复用    |
| 13 | `P13-integration/`       | P13 系统集成与接口对接   | 资源     | `postman/` + `screenshots/`                                             | 联调、排错              |
| 14 | `P14-perf-security/`     | P14 性能优化与安全加固   | 半代码   | `config/SecurityConfig.java` + `examples/`                              | Spring Security、BCrypt |
| 15 | `P15-uat/`               | P15 功能验收与项目展示   | 资源     | `examples/` + `templates/ppt-template.pptx`                             | 验收测试                |
| 16 | `P16-defense/`           | P16 项目答辩与课程总结   | 范例     | `examples/bookshop-defense-summary.md`                                  | 答辩                    |

**形态说明**：

- **后端工程**：含 `pom.xml`，可 `mvn spring-boot:run` 运行
- **前端工程**：含 `package.json`，可 `npm install && npm run dev` 运行
- **半代码**：含关键代码片段 + 测试 / 异常 / 文档， **不是完整工程**（独立运行时需自行整合）
- **资源**：纯 SQL / drawio / Postman / pptx / 文档，无运行入口

---

## 3. 子目录规范

各目录内部子目录命名遵循统一约定（与物理存放结构对齐）：

| 子目录                                                                             | 用途                                      | 示例                                                                                        |
|------------------------------------------------------------------------------------|-------------------------------------------|---------------------------------------------------------------------------------------------|
| `src/`                                                                             | 后端 / 前端工程源码（Spring Boot / Vue3） | `P02-bookshop-dao/src/`                                                                     |
| `service/` `controller/` `mapper/` `config/` `interceptor/` `exception/` `common/` | 后端分层包                                | `P04-bookshop-tx/service/`                                                                  |
| `views/` `components/` `stores/` `api/` `router/` `types/` `utils/`                | 前端分层目录                              | `P12-frontend-pages/views/`                                                                 |
| `frontend/` `backend/`                                                             | 双工程子目录                              | `P06-frontend-backend/frontend/`                                                            |
| `docs/`                                                                            | 教学补充文档（非 md 学生模板）            | `P08-redis-interceptor/docs/P08-test-401.md`                                                |
| `templates/`                                                                       | 非 md 模板资源（drawio / pptx）           | `P10-db-design/templates/er-diagram-template.drawio`、`P15-uat/templates/ppt-template.pptx` |
| `examples/`                                                                        | 范例（md / sql / java 片段）              | `P09-proposal/examples/bookshop-proposal-example.md`                                        |
| `sql/`                                                                             | 数据库脚本（如独立成目录）                | `P01-bookshop/bookshop.sql`（单文件）                                                       |
| `postman/`                                                                         | Postman 集合                              | `P11-backend-arch/postman/collection.json`                                                  |
| `screenshots/`                                                                     | 课堂演示截图                              | `P13-integration/screenshots/`                                                              |

> **重要**：上表中 `templates/` 一律指 **非 md 模板资源**（drawio、pptx 等）。
> **Markdown 学生模板已全部迁移到 `../文档模板/`**，本目录不再保留。

---

## 4. 运行说明

### 4.1 后端工程（P02 / P04 / P06 / P07 / P08 / P11 / P14）

```bash
# 前置：JDK 17、Maven 3.8+、MySQL 8.0
# 1. 先执行 P01 的 bookshop.sql 建库
mysql -u root -p bookshop < P01-bookshop/bookshop.sql

# 2. 修改 application.yml 里的数据库账号密码
cd P02-bookshop-dao
mvn spring-boot:run   # 默认 http://localhost:8080
```

> ⚠️ P04 / P07 / P08 / P11 / P14 是 **渐进式演进**目录，每个目录相对上一个加入新特性
> （事务 → 优化 → Redis → 鉴权 → 安全）。建议从 P02 开始逐步叠加，不要跳跃运行。

### 4.2 前端工程（P05 / P06 / P12）

```bash
# 前置：Node 18+
cd P05-bookshop-frontend
npm install
npm run dev           # 默认 http://localhost:5173
```

> P06 启前后端两套工程时，前端默认把 `/api` 代理到 `http://localhost:8080`，
> 详见 `P06-frontend-backend/README.md`。

### 4.3 资源与文档目录（P01 / P03 / P09 / P10 / P13 / P15 / P16）

无运行入口，直接用对应工具打开：

- `.sql` → MySQL CLI / Navicat / DBeaver
- `.svg` `.drawio` → draw.io / VS Code 插件
- `.puml` → PlantUML 渲染插件
- `.json`（Postman 集合）→ 导入 Postman
- `.pptx` → WPS / PowerPoint
- `.md` → VS Code / Typora / WorkBuddy 预览

---

## 5. 命名约定

| 层                 | 约定                                      | 值 / 示例                                                                |
|--------------------|-------------------------------------------|--------------------------------------------------------------------------|
| Java 包名          | 反域名                                    | `com.example.bookshop`                                                   |
| 数据库             | 小写无空格                                | `bookshop`（MySQL 8，utf8mb4）                                           |
| 表命名             | 小写下划线、`t_` 前缀                     | `t_user`、`t_book`、`t_order`、`t_order_item`、`t_address`、`t_category` |
| 字段命名           | 小写下划线                                | `user_name`、`created_at`                                                |
| 后端端口           | 8080                                      | `server.port: 8080`                                                      |
| 前端端口           | 5173                                      | Vite 默认                                                                |
| 后端工程文件名     | `P{课次}-{项目代号}/`                     | `P02-bookshop-dao/`                                                      |
| 前端工程文件名     | `P{课次}-{项目代号}/`                     | `P05-bookshop-frontend/`                                                 |
| 学生 Markdown 交付 | `P{课次}-{类型}-{组号}.md`                | `P01-项目立项说明书-G05.md`                                              |
| 证据文件           | `evidence/P{课次}-证据-{序号}-{简述}.png` | `evidence/P09-用例图.png`                                                |

---

## 6. 校验清单（每次发布前必过）

- [ ] 16 个目录齐全，与 `../P01-*.md` ~ `../P16-*.md` 逐一对应
- [ ] 后端工程 `pom.xml` 内 spring-boot-starter-parent 版本一致（避免混版本）
- [ ] `application.yml` 中的数据库账号密码已脱敏（不出现真实密码）
- [ ] `bookshop.sql` 在 MySQL 8.0 中可一次执行通过
- [ ] Postman 集合可正常导入并跑通所有接口
- [ ] `ppt-template.pptx` 可被 WPS / PowerPoint 正常打开
- [ ] `../文档模板/` 的 15 个模板与本目录无重复内容

---

## 7. 维护说明

- **加新目录**：课次编号严格沿用 P01–P16；新目录必须含 `README.md` 说明形态（后端工程 / 前端工程 / 半代码 / 资源）
- **模板迁移**：任何 md 模板 **只**写到 `../文档模板/`，本目录的 `docs/` `templates/` 一律禁止放学生交付物
- **冲突解决**：若某篇学生文档附录列出的文件路径与本目录不一致， **以本 README 为准**（本 README 是唯一权威目录说明）
- **版本演进**：P04 / P07 / P08 / P11 / P14 是 **叠加式**演进，不要复制整目录后只改一部分——保留演进脉络便于课堂演示对比