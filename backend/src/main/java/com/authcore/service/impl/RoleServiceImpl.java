package com.authcore.service.impl;

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
import com.authcore.mapper.SysPermissionMapper;
import com.authcore.mapper.SysRoleMapper;
import com.authcore.mapper.SysRolePermissionMapper;
import com.authcore.mapper.SysUserRoleMapper;
import com.authcore.service.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 角色业务服务实现（roles/001）。
 */
@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;

    public RoleServiceImpl(SysRoleMapper roleMapper,
                            SysUserRoleMapper userRoleMapper,
                            SysRolePermissionMapper rolePermissionMapper,
                            SysPermissionMapper permissionMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
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

    @Override
    public RoleVO getRoleById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(1001, "角色不存在");
        }
        return toVO(role);
    }

    @Override
    public RoleVO createRole(RoleCreateDTO dto) {
        if (dto.code() == null || !dto.code().matches("^ROLE_[A-Z0-9_]+$")) {
            throw new BusinessException(400, "code 必须以 ROLE_ 开头且仅含大写字母、数字、下划线");
        }

        if (roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getName, dto.name())) != null) {
            throw new BusinessException(1101, "角色名已存在");
        }

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

    @Override
    public RoleVO updateRole(Long id, RoleUpdateDTO dto) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(1001, "角色不存在");
        }

            if (dto.name() != null) {
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

        roleMapper.updateById(role);
        return toVO(role);
    }

    @Override
    public void deleteRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(1001, "角色不存在");
        }

        long userRefCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, id)
        );
        if (userRefCount > 0) {
            throw new BusinessException(1103, "角色关联用户，无法删除");
        }

        long permRefCount = rolePermissionMapper.selectCount(
                new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, id)
        );
        if (permRefCount > 0) {
            throw new BusinessException(1104, "角色关联权限，无法删除");
        }

        roleMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(1001, "角色不存在");
        }

        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<SysPermission> perms = permissionMapper.selectBatchIds(permissionIds);
            if (perms.size() != permissionIds.size()) {
                throw new BusinessException(1201, "权限不存在");
            }
        }

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

    @Override
    public List<PermissionSimpleVO> listAllPermissions() {
        List<SysPermission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .select(SysPermission::getId, SysPermission::getCode, SysPermission::getName)
                        .orderByAsc(SysPermission::getId)
        );
        return permissions.stream()
                .map(p -> new PermissionSimpleVO(p.getId(), p.getCode(), p.getName()))
                .collect(Collectors.toList());
    }
}