-- web/043: 补齐模块状态切换权限种子（module:toggle-status）
-- 背景：前端模块管理页 canToggleModuleStatus 门禁检查 module:toggle-status，
--       V6 种子 16 权限无此码导致状态切换按钮永不可见；本迁移幂等补齐权限与 ROLE_ADMIN 授权。
-- 幂等策略：利用唯一键冲突忽略（INSERT IGNORE），重放不产生重复数据。

INSERT IGNORE INTO sys_permission (module_id, name, code, description)
SELECT id, '模块状态切换', 'module:toggle-status', '启用或停用模块状态'
FROM sys_module WHERE code = 'module_mgmt';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'ROLE_ADMIN' AND p.code = 'module:toggle-status';
