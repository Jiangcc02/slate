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
