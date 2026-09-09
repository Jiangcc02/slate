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
