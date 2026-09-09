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
