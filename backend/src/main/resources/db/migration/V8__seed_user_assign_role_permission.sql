-- web/047: 补齐用户-角色分配权限种子（user:assign-role）
-- 背景：前端用户管理页角色分配按钮门禁检查 user:assign-role，V7 种子 17 权限无此码导致按钮永不可见；
--       本迁移幂等补齐权限（挂 user_mgmt 模块）与 ROLE_ADMIN 授权。
-- 幂等策略：利用唯一键冲突忽略（INSERT IGNORE），重放不产生重复数据。

INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '用户角色分配', 'user:assign-role', '为用户分配或取消角色'
FROM sys_module WHERE code = 'user_mgmt';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'ROLE_ADMIN' AND p.code = 'user:assign-role';
