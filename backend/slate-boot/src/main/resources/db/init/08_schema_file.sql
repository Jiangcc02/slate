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
