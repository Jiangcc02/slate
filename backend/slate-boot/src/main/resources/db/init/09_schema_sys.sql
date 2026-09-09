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
