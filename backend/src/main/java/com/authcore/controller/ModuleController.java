package com.authcore.controller;

import com.authcore.common.Result;
import com.authcore.dto.module.ModuleCreateDTO;
import com.authcore.dto.module.ModuleQueryDTO;
import com.authcore.dto.module.ModuleUpdateDTO;
import com.authcore.dto.module.ModuleVO;
import com.authcore.dto.permission.PermissionSimpleVO;
import com.authcore.dto.permission.PermissionVO;
import com.authcore.service.ModuleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模块管理控制器（modules/001）。
 */
@Tag(name = "模块管理", description = "模块 CRUD：分页查询、详情、权限级联查询、创建、更新、删除（引用保护）")
@RestController
@RequestMapping("/api/v1/modules")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    /**
     * 分页条件查询模块列表。
     *
     * @param page     页码（默认 1）
     * @param size     每页大小（默认 10，最大 100）
     * @param name     模块名模糊匹配
     * @param code     模块编码模糊匹配
     * @param status   状态：1 启用 / 0 停用
     * @return 分页结果：data={list,total,page,size}
     */
    @Operation(summary = "分页条件查询模块")
    @GetMapping
    public Result<IPage<ModuleVO>> queryModules(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Integer status
    ) {
        ModuleQueryDTO dto = new ModuleQueryDTO(page, size, name, code, status);
        IPage<ModuleVO> pageResult = moduleService.queryModules(dto);
        return Result.ok(pageResult);
    }

    /**
     * 获取模块详情。
     *
     * @param id 模块 ID
     * @return 模块视图对象
     */
    @Operation(summary = "获取模块详情")
    @GetMapping("/{id}")
    public Result<ModuleVO> getModuleById(@PathVariable Long id) {
        ModuleVO vo = moduleService.getModuleById(id);
        return Result.ok(vo);
    }

    /**
     * 模块下权限级联查询。
     *
     * @param id 模块 ID
     * @return 该模块下所有权限列表
     */
    @Operation(summary = "模块下权限级联查询")
    @GetMapping("/{id}/permissions")
    public Result<List<PermissionVO>> queryModulePermissions(@PathVariable Long id) {
        List<PermissionVO> permissions = moduleService.queryModulePermissions(id);
        return Result.ok(permissions);
    }

    /**
     * 创建模块。
     *
     * @param dto 创建参数（name、code、base_url、description、status）
     * @return 创建后的模块视图对象（含 id）
     */
    @Operation(summary = "创建模块，name/code 唯一校验")
    @PostMapping
    public Result<ModuleVO> createModule(@Valid @RequestBody ModuleCreateDTO dto) {
        ModuleVO vo = moduleService.createModule(dto);
        return Result.ok(vo);
    }

    /**
     * 更新模块（不含 code）。
     *
     * @param id  模块 ID
     * @param dto 更新参数（name、base_url、description、status）
     * @return 更新后的模块视图对象
     */
    @Operation(summary = "更新模块（不含 code）")
    @PutMapping("/{id}")
    public Result<ModuleVO> updateModule(@PathVariable Long id, @Valid @RequestBody ModuleUpdateDTO dto) {
        ModuleVO vo = moduleService.updateModule(id, dto);
        return Result.ok(vo);
    }

    /**
     * 删除模块（引用保护：存在 sys_permission 则拒绝，code=1302）。
     *
     * @param id 模块 ID
     * @return 空载荷成功响应
     */
    @Operation(summary = "删除模块（引用保护）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteModule(@PathVariable Long id) {
        moduleService.deleteModule(id);
        return Result.okMessage("删除成功");
    }

    /**
     * 查询所有权限精简列表（仅含 id、code、name）。
     *
     * @return 权限精简对象列表，供前端下拉选项使用
     */
    @Operation(summary = "查询所有权限精简列表（用于下拉选项）")
    @GetMapping("/permissions/all")
    public Result<List<PermissionSimpleVO>> listAllPermissions() {
        List<PermissionSimpleVO> permissions = moduleService.listAllPermissions();
        return Result.ok(permissions);
    }
}