-- 域/模块: 平台底座/启动器
-- 类型: Flyway 基线迁移
-- 职责: V1 基线=原 db/init 全部建表+种子脚本按原执行顺序合并（schema 01,03,05,07,08,09 → seed 02,04,06,10），新环境从本脚本全量建库
-- 设计文档: docs/design/平台底座/design.md
-- 维护者: 协调者 / agent-fffabc

-- ══════════ 原 db/init/01_schema.sql ══════════
-- 域/模块: 平台底座/认证授权
-- 类型: 数据库 DDL
-- 职责: 底座认证授权相关表（account/档案/RBAC/日志），实体列级设计见 docs/design/平台底座/detail/data.md
-- 设计文档: docs/design/平台底座/design.md
-- 维护者: 协调者 / agent-fffabc
-- 约定: 主键 bigint；种子数据 ID 保留段 1~999（运行时雪花 ID 为 41 位时间戳段，不冲突）
-- 库由 JDBC createDatabaseIfNotExist=true 自动创建，本脚本只建表（幂等可重复执行）

-- 登录账号（凭证与状态；与档案 1:1）
CREATE TABLE IF NOT EXISTS account (
  id            BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花 ID',
  username      VARCHAR(64)  NOT NULL COMMENT '登录名，全库唯一',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt',
  status        VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT 'active/locked/disabled',
  user_id       BIGINT       NOT NULL COMMENT '关联 user_profile.id',
  last_login_at DATETIME     NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by    BIGINT       NULL,
  updated_by    BIGINT       NULL,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_account_username (username),
  UNIQUE KEY uk_account_user (user_id)
) ENGINE = InnoDB COMMENT = '登录账号';

-- 用户主档（auth 阶段最小列；完整档案随用户中心模块扩展）
CREATE TABLE IF NOT EXISTS user_profile (
  id         BIGINT      NOT NULL PRIMARY KEY,
  real_name  VARCHAR(64) NOT NULL COMMENT '姓名（加密列改造随用户中心模块，见 detail/data.md）',
  user_type  VARCHAR(16) NOT NULL COMMENT 'STUDENT/TEACHER/GUARDIAN/STAFF',
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT      NULL,
  updated_by BIGINT      NULL,
  deleted    TINYINT     NOT NULL DEFAULT 0
) ENGINE = InnoDB COMMENT = '用户主档';

-- RBAC：角色（六预置角色只读，type=preset 不可改删）
CREATE TABLE IF NOT EXISTS role (
  id         BIGINT       NOT NULL PRIMARY KEY,
  code       VARCHAR(32)  NOT NULL COMMENT '角色码，如 ADMIN',
  name       VARCHAR(64)  NOT NULL,
  type       VARCHAR(16)  NOT NULL DEFAULT 'preset' COMMENT 'preset 预置只读 / custom 自定义',
  remark     VARCHAR(255) NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT       NULL,
  updated_by BIGINT       NULL,
  deleted    TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_role_code (code)
) ENGINE = InnoDB COMMENT = '角色';

-- RBAC：权限（菜单/按钮级树）
CREATE TABLE IF NOT EXISTS permission (
  id         BIGINT      NOT NULL PRIMARY KEY,
  code       VARCHAR(96) NOT NULL COMMENT '如 sys:role:write',
  name       VARCHAR(64) NOT NULL,
  type       VARCHAR(16) NOT NULL COMMENT 'menu/button',
  parent_id  BIGINT      NOT NULL DEFAULT 0,
  sort       INT         NOT NULL DEFAULT 0,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT      NULL,
  updated_by BIGINT      NULL,
  deleted    TINYINT     NOT NULL DEFAULT 0,
  UNIQUE KEY uk_permission_code (code)
) ENGINE = InnoDB COMMENT = '权限（菜单/按钮级）';

CREATE TABLE IF NOT EXISTS role_permission (
  id         BIGINT   NOT NULL PRIMARY KEY,
  role_id    BIGINT   NOT NULL,
  permission_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_rp (role_id, permission_id),
  KEY idx_rp_perm (permission_id)
) ENGINE = InnoDB COMMENT = '角色-权限授予';

CREATE TABLE IF NOT EXISTS account_role (
  id         BIGINT   NOT NULL PRIMARY KEY,
  account_id BIGINT   NOT NULL,
  role_id    BIGINT   NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ar (account_id, role_id),
  KEY idx_ar_role (role_id)
) ENGINE = InnoDB COMMENT = '账号-角色授予';

-- 登录日志（只追加，不提供修改/删除 API）
CREATE TABLE IF NOT EXISTS login_log (
  id         BIGINT       NOT NULL PRIMARY KEY,
  username   VARCHAR(64)  NOT NULL,
  account_id BIGINT       NULL COMMENT '成功时记录',
  success    TINYINT      NOT NULL COMMENT '1/0',
  fail_reason VARCHAR(32) NULL COMMENT 'BAD_CREDENTIALS/LOCKED/DISABLED',
  ip         VARCHAR(45)  NULL,
  user_agent VARCHAR(255) NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ll_username (username, created_at)
) ENGINE = InnoDB COMMENT = '登录日志';

-- 操作日志（只追加；@Audited 完整机制随后续任务包，表先就位）
CREATE TABLE IF NOT EXISTS operation_log (
  id            BIGINT       NOT NULL PRIMARY KEY,
  account_id    BIGINT       NULL,
  on_behalf_of  BIGINT       NULL COMMENT '双身份末端用户（api-conventions §2）',
  service_id    VARCHAR(64)  NULL COMMENT '服务调用方（Agent 运行时等）',
  action        VARCHAR(96)  NOT NULL,
  target        VARCHAR(128) NULL,
  params_digest VARCHAR(512) NULL COMMENT '脱敏后参数摘要',
  result        VARCHAR(8)   NOT NULL COMMENT 'ok/fail',
  trace_id      VARCHAR(32)  NULL,
  ip            VARCHAR(45)  NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ol_account (account_id, created_at),
  KEY idx_ol_trace (trace_id)
) ENGINE = InnoDB COMMENT = '操作审计日志';

-- ══════════ 原 db/init/03_schema_org.sql ══════════
-- 域/模块: 平台底座/组织架构
-- 类型: 数据库 DDL
-- 职责: 组织五级树、班级名单（异动留痕）、任课关系、领域事件 outbox（Agent-ready 三件套之一）
-- 设计文档: docs/design/平台底座/design.md
-- 维护者: 协调者 / agent-fffabc

-- 组织树（机构/校区/学部/年级/班级 五级；path 物化路径 '/{id}/{id}/…'，树查询走前缀）
CREATE TABLE IF NOT EXISTS org_unit (
  id         BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID',
  type       VARCHAR(16) NOT NULL COMMENT 'INSTITUTION/CAMPUS/SECTION/GRADE/CLASS',
  name       VARCHAR(64) NOT NULL,
  parent_id  BIGINT      NOT NULL DEFAULT 0 COMMENT '0=根',
  path       VARCHAR(255) NOT NULL COMMENT '物化路径 /1/5/9/',
  sort       INT         NOT NULL DEFAULT 0,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT      NULL,
  updated_by BIGINT      NULL,
  deleted    TINYINT     NOT NULL DEFAULT 0,
  KEY idx_org_parent (parent_id),
  KEY idx_org_path (path)
) ENGINE = InnoDB COMMENT = '组织架构五级树';

-- 班级名单（同一学生同学年同学级唯一在籍——业务校验 ORG-003；异动留痕不物理删）
CREATE TABLE IF NOT EXISTS class_membership (
  id            BIGINT      NOT NULL PRIMARY KEY,
  class_id      BIGINT      NOT NULL,
  student_id    BIGINT      NOT NULL COMMENT 'user_profile.id',
  academic_year VARCHAR(16) NOT NULL COMMENT '如 2026-2027',
  status        VARCHAR(16) NOT NULL DEFAULT 'ENROLLED' COMMENT 'ENROLLED 在籍/LEFT 转出/GRADUATED 毕业',
  joined_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  left_at       DATETIME    NULL,
  leave_reason  VARCHAR(128) NULL,
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_cm_class (class_id, academic_year, status),
  KEY idx_cm_student (student_id, academic_year)
) ENGINE = InnoDB COMMENT = '班级学生名单（异动留痕）';

-- 任课关系（教师×班级×课程×学年学期；course_id 引用课程域 ID 不复制字段，课程域校验随其模块接入）
CREATE TABLE IF NOT EXISTS teaching_assignment (
  id            BIGINT      NOT NULL PRIMARY KEY,
  teacher_id    BIGINT      NOT NULL COMMENT 'user_profile.id',
  class_id      BIGINT      NOT NULL,
  course_id     BIGINT      NOT NULL COMMENT '课程域 ID（引用不复制）',
  academic_year VARCHAR(16) NOT NULL,
  semester      TINYINT     NOT NULL DEFAULT 1 COMMENT '1 上学期 / 2 下学期',
  subject       VARCHAR(32) NOT NULL COMMENT '科目',
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ta (teacher_id, class_id, course_id, academic_year, semester),
  KEY idx_ta_teacher (teacher_id, academic_year),
  KEY idx_ta_class (class_id, academic_year)
) ENGINE = InnoDB COMMENT = '教师任课关系';

-- 领域事件 outbox（事务内落表，独立投递器轮询外发；Agent-ready：可观测）
CREATE TABLE IF NOT EXISTS domain_event (
  id             BIGINT       NOT NULL PRIMARY KEY,
  event_type     VARCHAR(64)  NOT NULL COMMENT '如 org.class.member-changed',
  aggregate_type VARCHAR(64)  NOT NULL,
  aggregate_id   BIGINT       NOT NULL,
  payload        TEXT         NOT NULL COMMENT 'JSON',
  status         VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENT/DEAD',
  retry_count    INT          NOT NULL DEFAULT 0,
  next_retry_at  DATETIME     NULL,
  sent_at        DATETIME     NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_de_status (status, next_retry_at)
) ENGINE = InnoDB COMMENT = '领域事件 outbox';

-- ══════════ 原 db/init/05_schema_user.sql ══════════
-- 域/模块: 平台底座/用户中心
-- 类型: 数据库 DDL
-- 职责: 角色子表（学生/教师/家长，敏感字段 AES 加密列）与家长-学生绑定（多对多，确认制）
-- 设计文档: docs/design/平台底座/design.md
-- 维护者: 协调者 / agent-fffabc
-- 加密决策（2026-09-09）：身份证/学籍号/家长手机号加密存储（AES-GCM，Base64 文本列）；
--   姓名明文存储+接口脱敏展示（检索需要 like，加密即丧失检索——core §7 脱敏义务不因此免除）

CREATE TABLE IF NOT EXISTS student_profile (
  user_id     BIGINT      NOT NULL PRIMARY KEY COMMENT 'user_profile.id',
  student_no  VARCHAR(256) NULL COMMENT '学籍号（AES-GCM 加密）',
  grade_entry VARCHAR(16) NULL COMMENT '入学届（如 2026）',
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB COMMENT = '学生子档';

CREATE TABLE IF NOT EXISTS teacher_profile (
  user_id    BIGINT      NOT NULL PRIMARY KEY,
  staff_no   VARCHAR(64) NULL COMMENT '职工号',
  subject    VARCHAR(32) NULL COMMENT '主教学科',
  title      VARCHAR(64) NULL COMMENT '职称',
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB COMMENT = '教师子档';

CREATE TABLE IF NOT EXISTS guardian_profile (
  user_id    BIGINT       NOT NULL PRIMARY KEY,
  phone_enc  VARCHAR(256) NULL COMMENT '手机号（AES-GCM 加密，绑定与触达凭据）',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB COMMENT = '家长子档';

-- 家长-学生绑定（多对多；confirmed 才允许家校域触达——合规：监护人同意）
CREATE TABLE IF NOT EXISTS guardian_student (
  id         BIGINT      NOT NULL PRIMARY KEY,
  guardian_id BIGINT     NOT NULL,
  student_id BIGINT     NOT NULL,
  relation   VARCHAR(16) NOT NULL COMMENT 'FATHER/MOTHER/GUARDIAN',
  is_primary TINYINT     NOT NULL DEFAULT 0,
  status     VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/confirmed/unbound',
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_gs (guardian_id, student_id),
  KEY idx_gs_student (student_id, status)
) ENGINE = InnoDB COMMENT = '家长-学生绑定（多对多，确认制）';

-- ══════════ 原 db/init/07_schema_msg.sql ══════════
-- 域/模块: 平台底座/消息中心
-- 类型: 数据库 DDL
-- 职责: 站内信（消息+投递记录）与公告；外部渠道（邮件/短信/微信）仅留适配器接口不建表
-- 设计文档: docs/design/平台底座/design.md
-- 维护者: 协调者 / agent-fffabc

CREATE TABLE IF NOT EXISTS message (
  id         BIGINT       NOT NULL PRIMARY KEY,
  title      VARCHAR(128) NOT NULL,
  content    VARCHAR(2000) NOT NULL,
  ref_type   VARCHAR(32)  NULL COMMENT '跳转引用类型（如 assignment）',
  ref_id     BIGINT       NULL COMMENT '跳转引用 ID',
  sender_id  BIGINT       NULL COMMENT '发送者（系统消息为空）',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_msg_created (created_at)
) ENGINE = InnoDB COMMENT = '站内信';

CREATE TABLE IF NOT EXISTS message_delivery (
  id          BIGINT      NOT NULL PRIMARY KEY,
  message_id  BIGINT      NOT NULL,
  receiver_id BIGINT      NOT NULL COMMENT 'user_profile.id',
  read_at     DATETIME    NULL COMMENT '空=未读',
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_md (message_id, receiver_id),
  KEY idx_md_receiver (receiver_id, read_at)
) ENGINE = InnoDB COMMENT = '站内信投递记录';

CREATE TABLE IF NOT EXISTS announcement (
  id          BIGINT       NOT NULL PRIMARY KEY,
  title       VARCHAR(128) NOT NULL,
  content     VARCHAR(4000) NOT NULL,
  scope_type  VARCHAR(16)  NOT NULL DEFAULT 'SCHOOL' COMMENT 'SCHOOL/CAMPUS/SECTION/GRADE/CLASS',
  scope_org_id BIGINT      NULL COMMENT '范围组织节点（SCHOOL 为空）',
  status      VARCHAR(16)  NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/OFFLINE',
  published_by BIGINT      NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ann_status (status, created_at)
) ENGINE = InnoDB COMMENT = '公告';

-- ══════════ 原 db/init/08_schema_file.sql ══════════
-- 域/模块: 平台底座/文件服务
-- 类型: 数据库 DDL
-- 职责: 文件元数据（对象存储实体归文件服务——data-ownership）
-- 设计文档: docs/design/平台底座/design.md
-- 维护者: 协调者 / agent-fffabc

CREATE TABLE IF NOT EXISTS file_object (
  id          BIGINT       NOT NULL PRIMARY KEY,
  bucket      VARCHAR(64)  NOT NULL,
  object_key  VARCHAR(255) NOT NULL COMMENT '对象键（预签名时生成，登记时校验一致）',
  biz_type    VARCHAR(32)  NOT NULL COMMENT '业务类型（白名单配置驱动）',
  biz_id      BIGINT       NULL COMMENT '业务引用 ID（引用中禁删）',
  file_name   VARCHAR(255) NOT NULL,
  content_type VARCHAR(128) NULL,
  size_bytes  BIGINT       NOT NULL,
  visibility  VARCHAR(16)  NOT NULL DEFAULT 'private' COMMENT 'private/public_signed',
  uploaded_by BIGINT       NULL,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_file_key (bucket, object_key),
  KEY idx_file_biz (biz_type, biz_id)
) ENGINE = InnoDB COMMENT = '文件元数据';

-- ══════════ 原 db/init/09_schema_sys.sql ══════════
-- 域/模块: 平台底座/系统管理
-- 类型: 数据库 DDL
-- 职责: 数据字典与参数配置（登录/操作日志表已在 01 建立）
-- 设计文档: docs/design/平台底座/design.md
-- 维护者: 协调者 / agent-fffabc

CREATE TABLE IF NOT EXISTS dict_type (
  id         BIGINT      NOT NULL PRIMARY KEY,
  code       VARCHAR(64) NOT NULL COMMENT '字典类型码，如 subject',
  name       VARCHAR(64) NOT NULL,
  remark     VARCHAR(255) NULL,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    TINYINT     NOT NULL DEFAULT 0,
  UNIQUE KEY uk_dt_code (code)
) ENGINE = InnoDB COMMENT = '字典类型';

CREATE TABLE IF NOT EXISTS dict_item (
  id         BIGINT      NOT NULL PRIMARY KEY,
  type_code  VARCHAR(64) NOT NULL,
  value      VARCHAR(64) NOT NULL COMMENT '存储值',
  label      VARCHAR(64) NOT NULL COMMENT '展示标签',
  sort       INT         NOT NULL DEFAULT 0,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    TINYINT     NOT NULL DEFAULT 0,
  UNIQUE KEY uk_di (type_code, value),
  KEY idx_di_type (type_code, sort)
) ENGINE = InnoDB COMMENT = '字典项';

CREATE TABLE IF NOT EXISTS sys_config (
  id         BIGINT       NOT NULL PRIMARY KEY,
  cfg_key    VARCHAR(96)  NOT NULL,
  cfg_value  VARCHAR(1024) NOT NULL,
  scope      VARCHAR(16)  NOT NULL DEFAULT 'GLOBAL' COMMENT 'GLOBAL/CAMPUS（校区码）',
  remark     VARCHAR(255) NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_cfg (cfg_key, scope)
) ENGINE = InnoDB COMMENT = '参数配置';

-- ══════════ 原 db/init/02_seed.sql ══════════
-- 域/模块: 平台底座/认证授权
-- 类型: 种子数据
-- 职责: 六预置角色、权限最小集、系统管理员账号（admin/admin123，首次登录后应改密）
-- 设计文档: docs/design/平台底座/design.md
-- 维护者: 协调者 / agent-fffabc
-- 种子 ID 保留段 1~999（运行时雪花 ID 不冲突）；INSERT IGNORE 幂等（启动重复执行不报错）
-- 库由 JDBC 自动创建，本脚本在 slate 库内执行（由 spring.sql.init 保证）

-- 六预置角色（core §2 六端角色；AUTH-006 保护预置角色不可改删）
INSERT IGNORE INTO role (id, code, name, type, remark) VALUES
  (1, 'STUDENT',      '学生', 'preset', '预置'),
  (2, 'TEACHER',      '任课教师', 'preset', '预置'),
  (3, 'HEAD_TEACHER', '班主任', 'preset', '预置'),
  (4, 'ACADEMIC',     '教务', 'preset', '预置'),
  (5, 'ADMIN',        '管理员', 'preset', '预置'),
  (6, 'PRINCIPAL',    '校长', 'preset', '预置');

-- 权限最小集（菜单/按钮级；业务域权限码随各域任务包登记追加）
INSERT IGNORE INTO permission (id, code, name, type, parent_id, sort) VALUES
  (101, 'sys',        '系统管理',   'menu',   0, 100),
  (111, 'sys:role',   '角色管理',   'menu',   101, 1),
  (112, 'sys:role:read',   '角色查询', 'button', 111, 1),
  (113, 'sys:role:write',  '角色维护', 'button', 111, 2),
  (121, 'sys:log',    '日志查询',   'menu',   101, 2),
  (122, 'sys:log:read',    '日志读取', 'button', 121, 1),
  (131, 'sys:dict:read',   '字典读取', 'button', 101, 3),
  (141, 'sys:config:read', '参数读取', 'button', 101, 4),
  (201, 'org',        '组织架构',   'menu',   0, 200),
  (211, 'org:member:read',  '班级名单查询', 'button', 201, 1),
  (301, 'user',       '用户中心',   'menu',   0, 300),
  (311, 'user:read',        '用户查询', 'button', 301, 1),
  (312, 'user:write',       '用户维护', 'button', 301, 2);

-- 角色授权：ADMIN 全量；ACADEMIC 组织与用户只读；其余角色业务权限随域任务包授予
INSERT IGNORE INTO role_permission (id, role_id, permission_id)
  SELECT 1000 + p.id, 5, p.id FROM permission p;

INSERT IGNORE INTO role_permission (id, role_id, permission_id) VALUES
  (2001, 4, 211), (2002, 4, 311);

-- 系统管理员：档案 + 账号 + 授予 ADMIN
INSERT IGNORE INTO user_profile (id, real_name, user_type) VALUES (1, '系统管理员', 'STAFF');
INSERT IGNORE INTO account (id, username, password_hash, status, user_id) VALUES
  (1, 'admin', '$2a$10$DTXSI7PuUczfb5YechbDluA1k22ZilMuWjVk1LIDyxPsO9MJsAmBC', 'active', 1);
INSERT IGNORE INTO account_role (id, account_id, role_id) VALUES (1, 1, 5);

-- ══════════ 原 db/init/04_seed_org.sql ══════════
-- 域/模块: 平台底座/组织架构
-- 类型: 种子数据
-- 职责: 组织权限码补种 + 示例学校五级树 + 教师/学生档案 + 示例名单与任课（幂等 INSERT IGNORE）
-- 设计文档: docs/design/平台底座/design.md
-- 维护者: 协调者 / agent-fffabc

-- 权限码补种（契约 detail/api/组织架构.md 权限列；dict/config 的 read 已在 02 种子，此处补 write）
INSERT IGNORE INTO permission (id, code, name, type, parent_id, sort) VALUES
  (212, 'org:member:write', '班级名单维护', 'button', 201, 2),
  (213, 'org:teaching',     '任课关系',     'menu',   201, 3),
  (214, 'org:teaching:read',  '任课查询',   'button', 213, 1),
  (215, 'org:teaching:write', '任课维护',   'button', 213, 2),
  (132, 'sys:dict:write',   '字典维护',     'button', 101, 3),
  (142, 'sys:config:write', '参数维护',     'button', 101, 4);

-- ADMIN 全量授权（新增权限一并补齐）
INSERT IGNORE INTO role_permission (id, role_id, permission_id)
  SELECT 3000 + p.id, 5, p.id FROM permission p;

-- 示例学校五级树：机构→校区→学部→年级→班级
INSERT IGNORE INTO org_unit (id, type, name, parent_id, path, sort) VALUES
  (10, 'INSTITUTION', '示例学校', 0,  '/10/', 1),
  (11, 'CAMPUS',      '本部校区', 10, '/10/11/', 1),
  (12, 'SECTION',     '小学部',   11, '/10/11/12/', 1),
  (13, 'SECTION',     '初中部',   11, '/10/11/13/', 2),
  (14, 'GRADE',       '一年级',   12, '/10/11/12/14/', 1),
  (15, 'CLASS',       '一(1)班',  14, '/10/11/12/14/15/', 1),
  (16, 'CLASS',       '一(2)班',  14, '/10/11/12/14/16/', 2);

-- 教师：档案 + 账号（teacher01/teacher02，密码 teacher123）+ TEACHER 角色
INSERT IGNORE INTO user_profile (id, real_name, user_type) VALUES
  (2, '李老师', 'TEACHER'),
  (3, '王老师', 'TEACHER');
INSERT IGNORE INTO account (id, username, password_hash, status, user_id) VALUES
  (2, 'teacher01', '$2a$10$JM23IjcxaIgVHHa1ESN9r.2GUeN80ie1jNGrT6Y8NjydQ.ESaPVrS', 'active', 2),
  (3, 'teacher02', '$2a$10$JM23IjcxaIgVHHa1ESN9r.2GUeN80ie1jNGrT6Y8NjydQ.ESaPVrS', 'active', 3);
INSERT IGNORE INTO account_role (id, account_id, role_id) VALUES
  (2, 2, 2), (3, 3, 2);

-- 学生档案（无账号——建档开户随用户中心模块；名单挂 user_profile.id）
INSERT IGNORE INTO user_profile (id, real_name, user_type) VALUES
  (4, '学生甲', 'STUDENT'), (5, '学生乙', 'STUDENT'), (6, '学生丙', 'STUDENT'),
  (7, '学生丁', 'STUDENT'), (8, '学生戊', 'STUDENT'), (9, '学生己', 'STUDENT');

-- 示例名单：一(1)班 2026-2027 学年，甲乙丙在籍
INSERT IGNORE INTO class_membership (id, class_id, student_id, academic_year, status) VALUES
  (11, 15, 4, '2026-2027', 'ENROLLED'),
  (12, 15, 5, '2026-2027', 'ENROLLED'),
  (13, 15, 6, '2026-2027', 'ENROLLED');

-- 示例任课：李老师教一(1)班 语文（course_id=9001 为示例课程 ID，课程域接入后由其校验）
INSERT IGNORE INTO teaching_assignment (id, teacher_id, class_id, course_id, academic_year, semester, subject) VALUES
  (21, 2, 15, 9001, '2026-2027', 1, '语文');

-- ══════════ 原 db/init/06_seed_user.sql ══════════
-- 域/模块: 平台底座/用户中心（含后续模块权限码一次种齐）
-- 类型: 种子数据
-- 职责: 用户中心/消息中心/文件服务/系统管理权限码 + ADMIN 授予（幂等）
-- 设计文档: docs/design/平台底座/design.md
-- 维护者: 协调者 / agent-fffabc

INSERT IGNORE INTO permission (id, code, name, type, parent_id, sort) VALUES
  -- 用户中心
  (313, 'user:import',    '用户批量导入', 'button', 301, 3),
  (314, 'user:lifecycle', '账号生命周期', 'button', 301, 4),
  (315, 'user:bind',      '家长绑定',     'menu',   301, 5),
  (316, 'user:bind:write','绑定维护',     'button', 315, 1),
  -- 消息中心
  (401, 'msg',            '消息中心',     'menu',   0, 400),
  (411, 'msg:announcement', '公告管理',   'menu',   401, 1),
  (412, 'msg:announcement:read',  '公告查询', 'button', 411, 1),
  (413, 'msg:announcement:write', '公告维护', 'button', 411, 2),
  -- 文件服务
  (501, 'file',           '文件服务',     'menu',   0, 500),
  (511, 'file:manage',    '文件管理',     'button', 501, 1);

-- ADMIN 全量授予（覆盖新旧全部权限码）
INSERT IGNORE INTO role_permission (id, role_id, permission_id)
  SELECT 3000 + p.id, 5, p.id FROM permission p;

-- ══════════ 原 db/init/10_seed_sys.sql ══════════
-- 域/模块: 平台底座/系统管理
-- 类型: 种子数据
-- 职责: 基础字典（学段/科目/学期）与示例参数（幂等）
-- 设计文档: docs/design/平台底座/design.md
-- 维护者: 协调者 / agent-fffabc

INSERT IGNORE INTO dict_type (id, code, name) VALUES
  (601, 'section',   '学段'),
  (602, 'subject',   '科目'),
  (603, 'semester',  '学期');

INSERT IGNORE INTO dict_item (id, type_code, value, label, sort) VALUES
  (611, 'section', 'PRIMARY',   '小学', 1),
  (612, 'section', 'JUNIOR',    '初中', 2),
  (613, 'section', 'SENIOR',    '高中', 3),
  (621, 'subject', 'CHINESE',   '语文', 1),
  (622, 'subject', 'MATH',      '数学', 2),
  (623, 'subject', 'ENGLISH',   '英语', 3),
  (624, 'subject', 'PHYSICS',   '物理', 4),
  (625, 'subject', 'CHEMISTRY', '化学', 5),
  (626, 'subject', 'BIOLOGY',   '生物', 6),
  (631, 'semester', '1',        '上学期', 1),
  (632, 'semester', '2',        '下学期', 2);

INSERT IGNORE INTO sys_config (id, cfg_key, cfg_value, scope, remark) VALUES
  (651, 'school.name',        '示例学校', 'GLOBAL', '学校名称（演示用）'),
  (652, 'file.publicBaseUrl', '',         'GLOBAL', '公共文件访问基址（预留）');

