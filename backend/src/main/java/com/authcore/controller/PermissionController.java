package com.authcore.controller;

import com.authcore.common.Result;
import com.authcore.dto.permission.ModuleVO;
import com.authcore.dto.permission.PermissionCreateDTO;
import com.authcore.dto.permission.PermissionQueryDTO;
import com.authcore.dto.permission.PermissionUpdateDTO;
import com.authcore.dto.permission.PermissionVO;
import com.authcore.service.PermissionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 权限管理控制器（perms/001）。
 */
@Tag(name = "权限管理", description = "权限 CRUD：分页查询、分组查询、详情、创建、更新、删除")
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 分页条件查询权限列表。
     *
     * @param page     页码（默认 1）
     * @param size     每页大小（默认 10，最大 100）
     * @param name     权限名模糊匹配
     * @param code     权限编码模糊匹配
     * @param moduleId 模块 ID
     * @return 分页结果：data={records,total,size,current,pages}
     */
    @Operation(summary = "分页条件查询权限")
    @GetMapping
    public Result<IPage<PermissionVO>> queryPermissions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Long moduleId
    ) {
        PermissionQueryDTO dto = new PermissionQueryDTO(page, size, name, code, moduleId);
        IPage<PermissionVO> pageResult = permissionService.queryPermissions(dto);
        return Result.ok(pageResult);
    }

    /**
     * 按模块分组查询权限。
     *
     * @return Map<ModuleVO, List<PermissionVO>>，每模块下含其权限列表
     */
    @Operation(summary = "按模块分组查询权限")
    @GetMapping("/grouped")
    public Result<Map<ModuleVO, List<PermissionVO>>> queryPermissionsGroupedByModule() {
        Map<ModuleVO, List<PermissionVO>> grouped = permissionService.queryPermissionsGroupedByModule();
        return Result.ok(grouped);
    }

    /**
     * 获取权限详情。
     *
     * @param id 权限 ID
     * @return 权限视图对象
     */
    @Operation(summary = "获取权限详情")
    @GetMapping("/{id}")
    public Result<PermissionVO> getPermissionById(@PathVariable Long id) {
        PermissionVO vo = permissionService.getPermissionById(id);
        return Result.ok(vo);
    }

    /**
     * 创建权限。
     *
     * @param dto 创建参数（name、code、module_id 必填）
     * @return 创建后的权限视图对象（含 id）
     */
    @Operation(summary = "创建权限，name/code 唯一校验、module_id 必填")
    @PostMapping
    public Result<PermissionVO> createPermission(@Valid @RequestBody PermissionCreateDTO dto) {
        PermissionVO vo = permissionService.createPermission(dto);
        return Result.ok(vo);
    }

    /**
     * 更新权限（不含 code）。
     *
     * @param id  权限 ID
     * @param dto 更新参数（name、description、module_id）
     * @return 更新后的权限视图对象
     */
    @Operation(summary = "更新权限（不含 code）")
    @PutMapping("/{id}")
    public Result<PermissionVO> updatePermission(@PathVariable Long id, @Valid @RequestBody PermissionUpdateDTO dto) {
        PermissionVO vo = permissionService.updatePermission(id, dto);
        return Result.ok(vo);
    }

    /**
     * 删除权限（引用保护：存在 sys_role_permission 则拒绝，code=1203）。
     *
     * @param id 权限 ID
     * @return 空载荷成功响应
     */
    @Operation(summary = "删除权限（引用保护）")
    @DeleteMapping("/{id}")
    public Result<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.okMessage("删除成功");
    }
}