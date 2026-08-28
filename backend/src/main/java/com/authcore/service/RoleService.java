package com.authcore.service;

import com.authcore.common.BusinessException;
import com.authcore.dto.role.RoleCreateDTO;
import com.authcore.dto.role.RoleQueryDTO;
import com.authcore.dto.role.RoleUpdateDTO;
import com.authcore.dto.role.RoleVO;
import com.authcore.entity.SysPermission;
import com.authcore.entity.SysRole;
import com.authcore.entity.SysRolePermission;
import com.authcore.entity.SysUserRole;
import com.authcore.mapper.SysPermissionMapper;
import com.authcore.mapper.SysRoleMapper;
import com.authcore.mapper.SysRolePermissionMapper;
import com.authcore.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色业务服务（roles/001）。
 */
@Service
@Transactional
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;

    public RoleService(SysRoleMapper roleMapper,
                       SysUserRoleMapper userRoleMapper,
                       SysRolePermissionMapper rolePermissionMapper,
                       SysPermissionMapper permissionMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
    }

    /**
     * 分页条件查询角色。
     *
     * @param dto 查询参数
     * @return 分页结果，含 list/total/page/size
     */
    public IPage<RoleVO> queryRoles(RoleQueryDTO dto) {
        Page<SysRole> page = new Page<>(dto.page(), dto.size());
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();

        if (dto.name() != null && !dto.name().isBlank()) {
            wrapper.like(SysRole::getName, dto.name());
        }
        if (dto.code() != null && !dto.code().isBlank()) {
            wrapper.like(SysRole::getCode, dto.code());
        }
        if (dto.status() != null) {
            wrapper.eq(SysRole::getStatus, dto.status());
        }

        wrapper.orderByDesc(SysRole::getCreatedAt);

        IPage<SysRole> resultPage = roleMapper.selectPage(page, wrapper);
        return resultPage.convert(this::toVO);
    }

    /**
     * 根据 ID 查询角色详情。
     *
     * @param id 角色 ID
     * @return 角色视图对象
     * @throws BusinessException code=1001 角色不存在
     */
    public RoleVO getRoleById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(1001, "角色不存在");
        }
        return toVO(role);
    }

    /**
     * 创建角色。
     *
     * @param dto 创建参数
     * @return 创建后的角色视图对象
     * @throws BusinessException code=1101 name 已存在；code=1102 code 已存在；code=400 code 格式不合规
     */
    public RoleVO createRole(RoleCreateDTO dto) {
        // code 格式校验（ROLE_ 开头，仅大写字母、数字、下划线）
        if (dto.code() == null || !dto.code().matches("^ROLE_[A-Z0-9_]+$")) {
            throw new BusinessException(400, "code 必须以 ROLE_ 开头且仅含大写字母、数字、下划线");
        }

        // 唯一性校验：name
        if (roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getName, dto.name())) != null) {
            throw new BusinessException(1101, "角色名已存在");
        }

        // 唯一性校验：code
        if (roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, dto.code())) != null) {
            throw new BusinessException(1102, "角色编码已存在");
        }

        SysRole role = new SysRole();
        role.setName(dto.name());
        role.setCode(dto.code());
        role.setStatus(dto.status());

        roleMapper.insert(role);
        return toVO(role);
    }

    /**
     * 更新角色（仅 name、status，不改 code）。
     *
     * @param id  角色 ID
     * @param dto 更新参数
     * @return 更新后的角色视图对象
     * @throws BusinessException code=1001 角色不存在；code=1101 name 已存在
     */
    public RoleVO updateRole(Long id, RoleUpdateDTO dto) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(1001, "角色不存在");
        }

        if (dto.name() != null) {
            // 唯一性校验：name（排除自己）
            Long existingId = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getName, dto.name())
                    .ne(SysRole::getId, id)) != null
                    ? roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                            .eq(SysRole::getName, dto.name())
                            .ne(SysRole::getId, id)).getId()
                    : null;
            if (existingId != null) {
                throw new BusinessException(1101, "角色名已存在");
            }
            role.setName(dto.name());
        }
        if (dto.status() != null) {
            role.setStatus(dto.status());
        }
        // code 不可改

        roleMapper.updateById(role);
        return toVO(role);
    }

    /**
     * 删除角色（引用保护：存在 sys_user_role 或 sys_role_permission 则拒绝）。
     *
     * @param id 角色 ID
     * @throws BusinessException code=1001 角色不存在；code=1103 存在用户引用；code=1104 存在权限引用
     */
    public void deleteRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(1001, "角色不存在");
        }

        // 检查 sys_user_role 引用
        long userRefCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, id)
        );
        if (userRefCount > 0) {
            throw new BusinessException(1103, "角色关联用户，无法删除");
        }

        // 检查 sys_role_permission 引用
        long permRefCount = rolePermissionMapper.selectCount(
                new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, id)
        );
        if (permRefCount > 0) {
            throw new BusinessException(1104, "角色关联权限，无法删除");
        }

        roleMapper.deleteById(id);
    }

    /**
     * 批量分配角色权限（全量替换）。
     *
     * @param roleId       角色 ID
     * @param permissionIds 权限 ID 列表
     * @throws BusinessException code=1001 角色不存在；code=1201 权限不存在
     */
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // 1. 校验角色存在
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(1001, "角色不存在");
        }

        // 2. 校验权限有效：每个 permissionId 在 sys_permission 存在
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<SysPermission> perms = permissionMapper.selectBatchIds(permissionIds);
            if (perms.size() != permissionIds.size()) {
                throw new BusinessException(1201, "权限不存在");
            }
        }

        // 3. 事务内：先删后增
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long pid : permissionIds) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(pid);
                rolePermissionMapper.insert(rp);
            }
        }
    }

    private RoleVO toVO(SysRole role) {
        return new RoleVO(
                role.getId(),
                role.getName(),
                role.getCode(),
                role.getStatus(),
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }
}