package com.authcore.controller;

import com.authcore.common.Result;
import com.authcore.dto.role.RoleCreateDTO;
import com.authcore.dto.role.RoleQueryDTO;
import com.authcore.dto.role.RoleUpdateDTO;
import com.authcore.dto.role.RoleVO;
import com.authcore.service.RoleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器（roles/001）。
 */
@Tag(name = "角色管理", description = "角色 CRUD：分页查询、详情、创建、更新、删除")
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 分页条件查询角色列表。
     *
     * @param page     页码（默认 1）
     * @param size     每页大小（默认 10，最大 100）
     * @param name     角色名模糊匹配
     * @param code     角色编码模糊匹配
     * @param status   状态（0/1）
     * @return 分页结果：data={list,total,page,size}
     */
    @Operation(summary = "分页条件查询角色")
    @GetMapping
    public Result<IPage<RoleVO>> queryRoles(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Integer status
    ) {
        RoleQueryDTO dto = new RoleQueryDTO(page, size, name, code, status);
        IPage<RoleVO> pageResult = roleService.queryRoles(dto);
        return Result.ok(pageResult);
    }

    /**
     * 获取角色详情。
     *
     * @param id 角色 ID
     * @return 角色视图对象
     */
    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public Result<RoleVO> getRoleById(@PathVariable Long id) {
        RoleVO vo = roleService.getRoleById(id);
        return Result.ok(vo);
    }

    /**
     * 创建角色。
     *
     * @param dto 创建参数（name、code 必填）
     * @return 创建后的角色视图对象（含 id）
     */
    @Operation(summary = "创建角色，name/code 唯一校验")
    @PostMapping
    public Result<RoleVO> createRole(@Valid @RequestBody RoleCreateDTO dto) {
        RoleVO vo = roleService.createRole(dto);
        return Result.ok(vo);
    }

    /**
     * 更新角色（不含 code）。
     *
     * @param id  角色 ID
     * @param dto 更新参数（name、status）
     * @return 更新后的角色视图对象
     */
    @Operation(summary = "更新角色（不含 code）")
    @PutMapping("/{id}")
    public Result<RoleVO> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        RoleVO vo = roleService.updateRole(id, dto);
        return Result.ok(vo);
    }

    /**
     * 删除角色（引用保护：存在 sys_user_role 或 sys_role_permission 则拒绝，code=1103/1104）。
     *
     * @param id 角色 ID
     * @return 空载荷成功响应
     */
    @Operation(summary = "删除角色（引用保护）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.okMessage("删除成功");
    }
}