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
