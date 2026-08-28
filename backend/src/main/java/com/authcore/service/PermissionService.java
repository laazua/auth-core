package com.authcore.service;

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
 * 权限业务服务（perms/001）。
 */
@Service
@Transactional
public class PermissionService {

    private final SysPermissionMapper permissionMapper;
    private final SysModuleMapper moduleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    public PermissionService(SysPermissionMapper permissionMapper,
                             SysModuleMapper moduleMapper,
                             SysRolePermissionMapper rolePermissionMapper) {
        this.permissionMapper = permissionMapper;
        this.moduleMapper = moduleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    /**
     * 分页条件查询权限。
     *
     * @param dto 查询参数
     * @return 分页结果，含 list/total/page/size
     */
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

    /**
     * 按模块分组查询权限。
     *
     * @return Map<ModuleVO, List<PermissionVO>>，按模块分组的权限列表
     */
    public Map<ModuleVO, List<PermissionVO>> queryPermissionsGroupedByModule() {
        // 1. 查询所有模块
        List<SysModule> modules = moduleMapper.selectList(
                new LambdaQueryWrapper<SysModule>().orderByAsc(SysModule::getId)
        );

        // 2. 查询所有权限
        List<SysPermission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getModuleId, SysPermission::getId)
        );

        // 3. 按 moduleId 分组
        Map<Long, List<PermissionVO>> permByModule = permissions.stream()
                .collect(Collectors.groupingBy(
                        SysPermission::getModuleId,
                        Collectors.mapping(this::toVO, Collectors.toList())
                ));

        // 4. 构建有序 Map，保持模块顺序
        Map<ModuleVO, List<PermissionVO>> result = new LinkedHashMap<>();
        for (SysModule module : modules) {
            ModuleVO moduleVO = new ModuleVO(module.getId(), module.getName(), module.getCode());
            List<PermissionVO> perms = permByModule.getOrDefault(module.getId(), List.of());
            result.put(moduleVO, perms);
        }

        return result;
    }

    /**
     * 根据 ID 查询权限详情。
     *
     * @param id 权限 ID
     * @return 权限视图对象
     * @throws BusinessException code=1001 权限不存在
     */
    public PermissionVO getPermissionById(Long id) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(1001, "权限不存在");
        }
        return toVO(permission);
    }

    /**
     * 创建权限。
     *
     * @param dto 创建参数
     * @return 创建后的权限视图对象
     * @throws BusinessException code=1201 name 已存在；code=1202 code 已存在；code=400 module_id 不存在
     */
    public PermissionVO createPermission(PermissionCreateDTO dto) {
        // 唯一性校验：name
        if (permissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getName, dto.name())) != null) {
            throw new BusinessException(1201, "权限名已存在");
        }

        // 唯一性校验：code
        if (permissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getCode, dto.code())) != null) {
            throw new BusinessException(1202, "权限编码已存在");
        }

        // module_id 存在性校验
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

    /**
     * 更新权限（仅 name、description、module_id，不改 code）。
     *
     * @param id  权限 ID
     * @param dto 更新参数
     * @return 更新后的权限视图对象
     * @throws BusinessException code=1001 权限不存在；code=1201 name 已存在；code=400 module_id 不存在
     */
    public PermissionVO updatePermission(Long id, PermissionUpdateDTO dto) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(1001, "权限不存在");
        }

        if (dto.name() != null) {
            // 唯一性校验：name（排除自己）
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
            // module_id 存在性校验
            SysModule module = moduleMapper.selectById(dto.moduleId());
            if (module == null) {
                throw new BusinessException(400, "module_id 不存在");
            }
            permission.setModuleId(dto.moduleId());
        }
        // code 不可改

        permissionMapper.updateById(permission);
        return toVO(permission);
    }

    /**
     * 删除权限（引用保护：存在 sys_role_permission 则拒绝）。
     *
     * @param id 权限 ID
     * @throws BusinessException code=1001 权限不存在；code=1203 存在角色权限引用
     */
    public void deletePermission(Long id) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(1001, "权限不存在");
        }

        // 检查 sys_role_permission 引用
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