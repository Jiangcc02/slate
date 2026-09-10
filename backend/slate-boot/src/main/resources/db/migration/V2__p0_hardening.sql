-- 域/模块: 平台底座/生产就绪加固
-- 类型: Flyway 迁移（V2）
-- 职责: 加密字段确定性哈希列（等值查询/唯一键）、日志表时间索引、名单唯一键兜底、收件箱分页索引
-- 设计文档: docs/design/平台底座/design.md
-- 维护者: 协调者 / agent-fffabc
-- 说明: 哈希列存量回填由应用侧 BackfillRunner 启动时执行（需 AES 解密原列，SQL 无法完成）；
--       唯一键允许多行 NULL，未回填行不受影响

-- 家长手机号：HMAC-SHA256 确定性哈希，绑定/查重改走本列（phone_enc 保留随机 IV 加密，负责解密展示）
ALTER TABLE guardian_profile
  ADD COLUMN phone_hash CHAR(64) NULL COMMENT '手机号 HMAC-SHA256 确定性哈希（等值查询与唯一键）' AFTER phone_enc;
ALTER TABLE guardian_profile
  ADD UNIQUE KEY uk_gp_phone_hash (phone_hash);

-- 学籍号：同上（批量导入去重、建档查重改走本列）
ALTER TABLE student_profile
  ADD COLUMN student_no_hash CHAR(64) NULL COMMENT '学籍号 HMAC-SHA256 确定性哈希（等值查询与唯一键）' AFTER student_no;
ALTER TABLE student_profile
  ADD UNIQUE KEY uk_sp_no_hash (student_no_hash);

-- 日志表时间维度查询兜底索引（管理端按时间范围检索、保留期清理任务）
ALTER TABLE login_log ADD KEY idx_ll_created (created_at);
ALTER TABLE operation_log ADD KEY idx_ol_created (created_at);

-- 班级名单：同一学生+学年+状态唯一在籍（并发导入/重复导入的 DB 兜底，此前仅业务层查后插）
ALTER TABLE class_membership
  ADD UNIQUE KEY uk_cm_year_status (student_id, academic_year, status);

-- 收件箱按 id 倒序 SQL 分页的排序索引
ALTER TABLE message_delivery ADD KEY idx_md_receiver_id (receiver_id, id);

-- 字典项/参数配置切换为物理删除（删除语义定稿 2026-09-10：变更历史由操作审计留痕）；
-- 去除 @TableLogic 前先清掉历史软删行，避免过滤消失后死行重现
DELETE FROM dict_item WHERE deleted = 1;
DELETE FROM sys_config WHERE deleted = 1;
