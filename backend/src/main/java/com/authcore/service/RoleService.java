package com.authcore.service;

import com.authcore.common.BusinessException;
import com.authcore.dto.permission.PermissionSimpleVO;
import com.authcore.dto.role.RoleCreateDTO;
import com.authcore.dto.role.RoleQueryDTO;
import com.authcore.dto.role.RoleUpdateDTO;
import com.authcore.dto.role.RoleVO;
import com.authcore.entity.SysPermission;
import com.authcore.entity.SysRole;
import com.authcore.entity.SysRolePermission;
import com.authcore.entity.SysUserRole;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 角色业务服务接口（roles/001）。
 */
public interface RoleService {

    /**
     * 分页条件查询角色。
     *
     * @param dto 查询参数
     * @return 分页结果，含 list/total/page/size
     */
    IPage<RoleVO> queryRoles(RoleQueryDTO dto);

    /**
     * 根据 ID 查询角色详情。
     *
     * @param id 角色 ID
     * @return 角色视图对象
     * @throws BusinessException code=1001 角色不存在
     */
    RoleVO getRoleById(Long id);

    /**
     * 创建角色。
     *
     * @param dto 创建参数
     * @return 创建后的角色视图对象
     * @throws BusinessException code=1101 name 已存在；code=1102 code 已存在；code=400 code 格式不合规
     */
    RoleVO createRole(RoleCreateDTO dto);

    /**
     * 更新角色（仅 name、status，不改 code）。
     *
     * @param id  角色 ID
     * @param dto 更新参数
     * @return 更新后的角色视图对象
     * @throws BusinessException code=1001 角色不存在；code=1101 name 已存在
     */
    RoleVO updateRole(Long id, RoleUpdateDTO dto);

    /**
     * 删除角色（引用保护：存在 sys_user_role 或 sys_role_permission 则拒绝）。
     *
     * @param id 角色 ID
     * @throws BusinessException code=1001 角色不存在；code=1103 存在用户引用；code=1104 存在权限引用
     */
    void deleteRole(Long id);

    /**
     * 批量分配角色权限（全量替换）。
     *
     * @param roleId      角色 ID
     * @param permissionIds 权限 ID 列表
     * @throws BusinessException code=1001 角色不存在；code=1201 权限不存在
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 查询所有权限精简列表（仅含 id、code、name）。
     *
     * @return 权限精简对象列表，供前端下拉选项使用
     */
    List<PermissionSimpleVO> listAllPermissions();
}