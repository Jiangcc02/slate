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
