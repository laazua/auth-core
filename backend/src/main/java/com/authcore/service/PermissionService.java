package com.authcore.service;

import com.authcore.common.BusinessException;
import com.authcore.dto.permission.ModuleVO;
import com.authcore.dto.permission.PermissionCreateDTO;
import com.authcore.dto.permission.PermissionQueryDTO;
import com.authcore.dto.permission.PermissionUpdateDTO;
import com.authcore.dto.permission.PermissionVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

/**
 * 权限业务服务接口（perms/001）。
 */
public interface PermissionService {

    /**
     * 分页条件查询权限。
     *
     * @param dto 查询参数
     * @return 分页结果，含 list/total/page/size
     */
    IPage<PermissionVO> queryPermissions(PermissionQueryDTO dto);

    /**
     * 按模块分组查询权限。
     *
     * @return Map<ModuleVO, List<PermissionVO>>，按模块分组的权限列表
     */
    Map<ModuleVO, List<PermissionVO>> queryPermissionsGroupedByModule();

    /**
     * 根据 ID 查询权限详情。
     *
     * @param id 权限 ID
     * @return 权限视图对象
     * @throws BusinessException code=1001 权限不存在
     */
    PermissionVO getPermissionById(Long id);

    /**
     * 创建权限。
     *
     * @param dto 创建参数
     * @return 创建后的权限视图对象
     * @throws BusinessException code=1201 name 已存在；code=1202 code 已存在；code=400 module_id 不存在
     */
    PermissionVO createPermission(PermissionCreateDTO dto);

    /**
     * 更新权限（仅 name、description、module_id，不改 code）。
     *
     * @param id  权限 ID
     * @param dto 更新参数
     * @return 更新后的权限视图对象
     * @throws BusinessException code=1001 权限不存在；code=1201 name 已存在；code=400 module_id 不存在
     */
    PermissionVO updatePermission(Long id, PermissionUpdateDTO dto);

    /**
     * 删除权限（引用保护：存在 sys_role_permission 则拒绝）。
     *
     * @param id 权限 ID
     * @throws BusinessException code=1001 权限不存在；code=1203 存在角色权限引用
     */
    void deletePermission(Long id);
}