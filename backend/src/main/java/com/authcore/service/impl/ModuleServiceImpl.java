package com.authcore.service.impl;

import com.authcore.common.BusinessException;
import com.authcore.dto.module.ModuleCreateDTO;
import com.authcore.dto.module.ModuleQueryDTO;
import com.authcore.dto.module.ModuleUpdateDTO;
import com.authcore.dto.module.ModuleVO;
import com.authcore.dto.permission.PermissionSimpleVO;
import com.authcore.dto.permission.PermissionVO;
import com.authcore.entity.SysModule;
import com.authcore.entity.SysPermission;
import com.authcore.mapper.SysModuleMapper;
import com.authcore.mapper.SysPermissionMapper;
import com.authcore.service.ModuleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 模块业务服务实现（modules/001）。
 */
@Service
@Transactional
public class ModuleServiceImpl implements ModuleService {

    private final SysModuleMapper moduleMapper;
    private final SysPermissionMapper permissionMapper;

    public ModuleServiceImpl(SysModuleMapper moduleMapper, SysPermissionMapper permissionMapper) {
        this.moduleMapper = moduleMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public IPage<ModuleVO> queryModules(ModuleQueryDTO dto) {
        Page<SysModule> page = new Page<>(dto.page(), dto.size());
        LambdaQueryWrapper<SysModule> wrapper = new LambdaQueryWrapper<>();

        if (dto.name() != null && !dto.name().isBlank()) {
            wrapper.like(SysModule::getName, dto.name());
        }
        if (dto.code() != null && !dto.code().isBlank()) {
            wrapper.like(SysModule::getCode, dto.code());
        }
        if (dto.status() != null) {
            wrapper.eq(SysModule::getStatus, dto.status());
        }

        wrapper.orderByDesc(SysModule::getCreatedAt);

        IPage<SysModule> resultPage = moduleMapper.selectPage(page, wrapper);
        return resultPage.convert(this::toVO);
    }

    @Override
    public ModuleVO getModuleById(Long id) {
        SysModule module = moduleMapper.selectById(id);
        if (module == null) {
            throw new BusinessException(1001, "模块不存在");
        }
        return toVO(module);
    }

    @Override
    public ModuleVO createModule(ModuleCreateDTO dto) {
        // 唯一性校验：name
        if (moduleMapper.selectOne(new LambdaQueryWrapper<SysModule>()
                .eq(SysModule::getName, dto.name())) != null) {
            throw new BusinessException(1301, "模块名已存在");
        }

        // 唯一性校验：code
        if (moduleMapper.selectOne(new LambdaQueryWrapper<SysModule>()
                .eq(SysModule::getCode, dto.code())) != null) {
            throw new BusinessException(1302, "模块编码已存在");
        }

        SysModule module = new SysModule();
        module.setName(dto.name());
        module.setCode(dto.code());
        module.setBaseUrl(dto.baseUrl());
        module.setDescription(dto.description());
        module.setStatus(dto.status() != null ? dto.status() : 1);

        moduleMapper.insert(module);
        return toVO(module);
    }

    @Override
    public ModuleVO updateModule(Long id, ModuleUpdateDTO dto) {
        SysModule module = moduleMapper.selectById(id);
        if (module == null) {
            throw new BusinessException(1001, "模块不存在");
        }

        if (dto.name() != null) {
            // 唯一性校验：name（排除自己）
            Long existingId = moduleMapper.selectOne(new LambdaQueryWrapper<SysModule>()
                    .eq(SysModule::getName, dto.name())
                    .ne(SysModule::getId, id)) != null
                    ? moduleMapper.selectOne(new LambdaQueryWrapper<SysModule>()
                            .eq(SysModule::getName, dto.name())
                            .ne(SysModule::getId, id)).getId()
                    : null;
            if (existingId != null) {
                throw new BusinessException(1301, "模块名已存在");
            }
            module.setName(dto.name());
        }
        if (dto.baseUrl() != null) {
            module.setBaseUrl(dto.baseUrl());
        }
        if (dto.description() != null) {
            module.setDescription(dto.description());
        }
        if (dto.status() != null) {
            module.setStatus(dto.status());
        }
        // code 不可改

        moduleMapper.updateById(module);
        return toVO(module);
    }

    @Override
    public List<PermissionVO> queryModulePermissions(Long moduleId) {
        List<SysPermission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getModuleId, moduleId)
                        .orderByAsc(SysPermission::getId)
        );
        return permissions.stream()
                .map(this::toPermissionVO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteModule(Long id) {
        SysModule module = moduleMapper.selectById(id);
        if (module == null) {
            throw new BusinessException(1001, "模块不存在");
        }

        // 检查 sys_permission 引用
        long refCount = permissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getModuleId, id)
        );
        if (refCount > 0) {
            throw new BusinessException(1302, "模块关联权限，无法删除");
        }

        moduleMapper.deleteById(id);
    }

    @Override
    public List<PermissionSimpleVO> listAllPermissions() {
        List<SysModule> modules = moduleMapper.selectList(
                new LambdaQueryWrapper<SysModule>()
                        .select(SysModule::getId, SysModule::getCode, SysModule::getName)
                        .orderByAsc(SysModule::getId)
        );
        return modules.stream()
                .map(m -> new PermissionSimpleVO(m.getId(), m.getCode(), m.getName()))
                .collect(Collectors.toList());
    }

    private ModuleVO toVO(SysModule module) {
        return new ModuleVO(
                module.getId(),
                module.getName(),
                module.getCode(),
                module.getBaseUrl(),
                module.getDescription(),
                module.getStatus(),
                module.getCreatedAt(),
                module.getUpdatedAt()
        );
    }

    private PermissionVO toPermissionVO(SysPermission permission) {
        return new PermissionVO(
                permission.getId(),
                permission.getModuleId(),
                "", // moduleName not needed for cascade query
                permission.getName(),
                permission.getCode(),
                permission.getDescription(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }
}