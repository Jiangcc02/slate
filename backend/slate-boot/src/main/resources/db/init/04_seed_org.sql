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
