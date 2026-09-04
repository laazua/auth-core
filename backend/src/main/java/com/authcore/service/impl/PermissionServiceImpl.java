package com.authcore.service.impl;

import com.authcore.common.BusinessException;
import com.authcore.dto.permission.ModuleVO;
import com.authcore.dto.permission.PermissionCreateDTO;
import com.authcore.dto.permission.PermissionQueryDTO;
import com.authcore.dto.permission.PermissionUpdateDTO;
import com.authcore.dto.permission.PermissionVO;
import com.authcore.entity.SysModule;
import com.authcore.entity.SysPermission;
import com.authcore.entity.SysRolePermission;
import com.authcore.mapper.SysModuleMapper;
import com.authcore.mapper.SysPermissionMapper;
import com.authcore.mapper.SysRolePermissionMapper;
import com.authcore.service.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限业务服务实现（perms/001）。
 */
@Service
@Transactional
public class PermissionServiceImpl implements PermissionService {

    private final SysPermissionMapper permissionMapper;
    private final SysModuleMapper moduleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    public PermissionServiceImpl(SysPermissionMapper permissionMapper,
                                   SysModuleMapper moduleMapper,
                                   SysRolePermissionMapper rolePermissionMapper) {
        this.permissionMapper = permissionMapper;
        this.moduleMapper = moduleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Override
    public IPage<PermissionVO> queryPermissions(PermissionQueryDTO dto) {
        Page<SysPermission> page = new Page<>(dto.page(), dto.size());
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();

        if (dto.name() != null && !dto.name().isBlank()) {
            wrapper.like(SysPermission::getName, dto.name());
        }
        if (dto.code() != null && !dto.code().isBlank()) {
            wrapper.like(SysPermission::getCode, dto.code());
        }
        if (dto.moduleId() != null) {
            wrapper.eq(SysPermission::getModuleId, dto.moduleId());
        }

        wrapper.orderByDesc(SysPermission::getCreatedAt);

        IPage<SysPermission> resultPage = permissionMapper.selectPage(page, wrapper);
        return resultPage.convert(this::toVO);
    }

    @Override
    public Map<ModuleVO, List<PermissionVO>> queryPermissionsGroupedByModule() {
        List<SysModule> modules = moduleMapper.selectList(
                new LambdaQueryWrapper<SysModule>().orderByAsc(SysModule::getId)
        );

        List<SysPermission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getModuleId, SysPermission::getId)
        );

        Map<Long, List<PermissionVO>> permByModule = permissions.stream()
                .collect(Collectors.groupingBy(
                        SysPermission::getModuleId,
                        Collectors.mapping(this::toVO, Collectors.toList())
                ));

        Map<ModuleVO, List<PermissionVO>> result = new LinkedHashMap<>();
        for (SysModule module : modules) {
            ModuleVO moduleVO = new ModuleVO(module.getId(), module.getName(), module.getCode());
            List<PermissionVO> perms = permByModule.getOrDefault(module.getId(), List.of());
            result.put(moduleVO, perms);
        }

        return result;
    }

    @Override
    public PermissionVO getPermissionById(Long id) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(1001, "权限不存在");
        }
        return toVO(permission);
    }

    @Override
    public PermissionVO createPermission(PermissionCreateDTO dto) {
        if (permissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getName, dto.name())) != null) {
            throw new BusinessException(1201, "权限名已存在");
        }

        if (permissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getCode, dto.code())) != null) {
            throw new BusinessException(1202, "权限编码已存在");
        }

        SysModule module = moduleMapper.selectById(dto.moduleId());
        if (module == null) {
            throw new BusinessException(400, "module_id 不存在");
        }

        SysPermission permission = new SysPermission();
        permission.setModuleId(dto.moduleId());
        permission.setName(dto.name());
        permission.setCode(dto.code());
        permission.setDescription(dto.description());

        permissionMapper.insert(permission);
        return toVO(permission);
    }

    @Override
    public PermissionVO updatePermission(Long id, PermissionUpdateDTO dto) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(1001, "权限不存在");
        }

        if (dto.name() != null) {
            Long existingId = permissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>()
                    .eq(SysPermission::getName, dto.name())
                    .ne(SysPermission::getId, id)) != null
                    ? permissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>()
                            .eq(SysPermission::getName, dto.name())
                            .ne(SysPermission::getId, id)).getId()
                    : null;
            if (existingId != null) {
                throw new BusinessException(1201, "权限名已存在");
            }
            permission.setName(dto.name());
        }
        if (dto.description() != null) {
            permission.setDescription(dto.description());
        }
        if (dto.moduleId() != null) {
            SysModule module = moduleMapper.selectById(dto.moduleId());
            if (module == null) {
                throw new BusinessException(400, "module_id 不存在");
            }
            permission.setModuleId(dto.moduleId());
        }

        permissionMapper.updateById(permission);
        return toVO(permission);
    }

    @Override
    public void deletePermission(Long id) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(1001, "权限不存在");
        }

        long refCount = rolePermissionMapper.selectCount(
                new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getPermissionId, id)
        );
        if (refCount > 0) {
            throw new BusinessException(1203, "权限关联角色，无法删除");
        }

        permissionMapper.deleteById(id);
    }

    private PermissionVO toVO(SysPermission permission) {
        SysModule module = moduleMapper.selectById(permission.getModuleId());
        String moduleName = module != null ? module.getName() : "";
        return new PermissionVO(
                permission.getId(),
                permission.getModuleId(),
                moduleName,
                permission.getName(),
                permission.getCode(),
                permission.getDescription(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }
}