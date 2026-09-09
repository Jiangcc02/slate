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
