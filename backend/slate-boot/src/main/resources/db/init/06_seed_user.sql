-- 域/模块: 平台底座/用户中心（含后续模块权限码一次种齐）
-- 类型: 种子数据
-- 职责: 用户中心/消息中心/文件服务/系统管理权限码 + ADMIN 授予（幂等）
-- 设计文档: docs/design/平台底座/design.md
-- 维护者: 协调者 / agent-fffabc

INSERT IGNORE INTO permission (id, code, name, type, parent_id, sort) VALUES
  -- 用户中心
  (313, 'user:import',    '用户批量导入', 'button', 301, 3),
  (314, 'user:lifecycle', '账号生命周期', 'button', 301, 4),
  (315, 'user:bind',      '家长绑定',     'menu',   301, 5),
  (316, 'user:bind:write','绑定维护',     'button', 315, 1),
  -- 消息中心
  (401, 'msg',            '消息中心',     'menu',   0, 400),
  (411, 'msg:announcement', '公告管理',   'menu',   401, 1),
  (412, 'msg:announcement:read',  '公告查询', 'button', 411, 1),
  (413, 'msg:announcement:write', '公告维护', 'button', 411, 2),
  -- 文件服务
  (501, 'file',           '文件服务',     'menu',   0, 500),
  (511, 'file:manage',    '文件管理',     'button', 501, 1);

-- ADMIN 全量授予（覆盖新旧全部权限码）
INSERT IGNORE INTO role_permission (id, role_id, permission_id)
  SELECT 3000 + p.id, 5, p.id FROM permission p;
