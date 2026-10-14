-- web/050: 补齐角色-权限分配按钮门禁种子（role:assign-permission）
-- 背景：前端角色管理页权限分配按钮门禁检查 role:assign-permission，V8 种子 18 权限无此码导致按钮永不可见；
--       本迁移幂等补齐权限（挂 role_mgmt 模块）与 ROLE_ADMIN 授权。
-- 幂等策略：利用唯一键冲突忽略（INSERT IGNORE），重放不产生重复数据。

INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '角色权限分配', 'role:assign-permission', '为角色分配或取消权限'
FROM sys_module WHERE code = 'role_mgmt';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'ROLE_ADMIN' AND p.code = 'role:assign-permission';
