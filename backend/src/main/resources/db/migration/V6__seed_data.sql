-- model/006：种子数据迁移（内置 admin + 示例角色/权限/模块）
-- 预置明文口令：admin123456
-- BCrypt 哈希（cost=10）：$2a$10$thO4/joMz94gBoPayCMfwO7xh/bPiMqefMx7mV39DtEDLy5Zq3fCG
-- 幂等策略：利用唯一键冲突忽略（INSERT IGNORE），重放不产生重复数据

-- 1. sys_user: 管理员用户
INSERT IGNORE INTO sys_user (username, password, nickname, email, phone, status)
VALUES ('admin', '$2a$10$thO4/joMz94gBoPayCMfwO7xh/bPiMqefMx7mV39DtEDLy5Zq3fCG', '管理员', 'admin@example.com', '13800000000', 1);

-- 2. sys_module: 4 个示例模块
INSERT IGNORE INTO sys_module (name, code, base_url, description, status) VALUES
('用户管理', 'user_mgmt', '/api/users', '用户增删改查、角色分配', 1),
('角色管理', 'role_mgmt', '/api/roles', '角色增删改查、权限分配', 1),
('权限管理', 'perm_mgmt', '/api/permissions', '权限增删改查、模块分组', 1),
('模块管理', 'module_mgmt', '/api/modules', '模块增删改查、权限级联', 1);

-- 3. sys_role: 2 个角色
INSERT IGNORE INTO sys_role (name, code, status) VALUES
('管理员', 'ROLE_ADMIN', 1),
('普通用户', 'ROLE_USER', 1);

-- 4. sys_permission: 14 个权限（每模块 3-4 个）
-- user_mgmt 模块权限
INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '用户查看', 'user:view', '查看用户列表与详情' FROM sys_module WHERE code = 'user_mgmt';

INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '用户新增', 'user:create', '创建新用户' FROM sys_module WHERE code = 'user_mgmt';

INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '用户修改', 'user:update', '修改用户信息' FROM sys_module WHERE code = 'user_mgmt';

INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '用户删除', 'user:delete', '删除用户' FROM sys_module WHERE code = 'user_mgmt';

-- role_mgmt 模块权限
INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '角色查看', 'role:view', '查看角色列表与详情' FROM sys_module WHERE code = 'role_mgmt';

INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '角色新增', 'role:create', '创建新角色' FROM sys_module WHERE code = 'role_mgmt';

INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '角色修改', 'role:update', '修改角色信息' FROM sys_module WHERE code = 'role_mgmt';

INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '角色删除', 'role:delete', '删除角色' FROM sys_module WHERE code = 'role_mgmt';

-- perm_mgmt 模块权限
INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '权限查看', 'perm:view', '查看权限列表与详情' FROM sys_module WHERE code = 'perm_mgmt';

INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '权限新增', 'perm:create', '创建新权限' FROM sys_module WHERE code = 'perm_mgmt';

INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '权限修改', 'perm:update', '修改权限信息' FROM sys_module WHERE code = 'perm_mgmt';

INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '权限删除', 'perm:delete', '删除权限' FROM sys_module WHERE code = 'perm_mgmt';

-- module_mgmt 模块权限
INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '模块查看', 'module:view', '查看模块列表与详情' FROM sys_module WHERE code = 'module_mgmt';

INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '模块新增', 'module:create', '创建新模块' FROM sys_module WHERE code = 'module_mgmt';

INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '模块修改', 'module:update', '修改模块信息' FROM sys_module WHERE code = 'module_mgmt';

INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '模块删除', 'module:delete', '删除模块' FROM sys_module WHERE code = 'module_mgmt';

-- 5. sys_user_role: admin 用户 -> admin 角色
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.code = 'ROLE_ADMIN';

-- 6. sys_role_permission: admin 角色 -> 全部 14 个权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'ROLE_ADMIN';

-- 7. sys_role_permission: user 角色 -> 仅只读权限（4 个 view 类权限）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'ROLE_USER' AND p.code LIKE '%:view';