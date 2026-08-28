package com.authcore.controller;

import com.authcore.common.Result;
import com.authcore.dto.user.UserCreateDTO;
import com.authcore.dto.user.UserQueryDTO;
import com.authcore.dto.user.UserRoleAssignDTO;
import com.authcore.dto.user.UserStatusDTO;
import com.authcore.dto.user.UserUpdateDTO;
import com.authcore.dto.user.UserVO;
import com.authcore.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器（users/001）。
 */
@Tag(name = "用户管理", description = "用户 CRUD：分页查询、详情、创建、更新、启停用、删除")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 分页条件查询用户列表。
     *
     * @param page     页码（默认 1）
     * @param size     每页大小（默认 10，最大 100）
     * @param username 用户名模糊匹配
     * @param nickname 昵称模糊匹配
     * @param email    邮箱模糊匹配
     * @param phone    手机号模糊匹配
     * @param status   状态（0/1）
     * @return 分页结果：data={list,total,page,size}
     */
    @Operation(summary = "分页条件查询用户")
    @GetMapping
    public Result<IPage<UserVO>> queryUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status
    ) {
        UserQueryDTO dto = new UserQueryDTO(page, size, username, nickname, email, phone, status);
        IPage<UserVO> pageResult = userService.queryUsers(dto);
        return Result.ok(pageResult);
    }

    /**
     * 获取用户详情。
     *
     * @param id 用户 ID
     * @return 用户视图对象
     */
    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        UserVO vo = userService.getUserById(id);
        return Result.ok(vo);
    }

    /**
     * 创建用户。
     *
     * @param dto 创建参数（username、password 必填）
     * @return 创建后的用户视图对象（含 id）
     */
    @Operation(summary = "创建用户，密码自动 BCrypt 加密")
    @PostMapping
    public Result<UserVO> createUser(@Valid @RequestBody UserCreateDTO dto) {
        UserVO vo = userService.createUser(dto);
        return Result.ok(vo);
    }

    /**
     * 更新用户（不含 username 与 password）。
     *
     * @param id  用户 ID
     * @param dto 更新参数（nickname、email、phone）
     * @return 更新后的用户视图对象
     */
    @Operation(summary = "更新用户（不含 username 与 password）")
    @PutMapping("/{id}")
    public Result<UserVO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        UserVO vo = userService.updateUser(id, dto);
        return Result.ok(vo);
    }

    /**
     * 切换用户状态（启用/停用）。
     *
     * @param id     用户 ID
     * @param dto    状态参数（status: 0=停用，1=启用）
     * @return 空载荷成功响应
     */
    @Operation(summary = "启停用用户")
    @PatchMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        userService.toggleStatus(id, dto.status());
        return Result.okMessage("状态更新成功");
    }

    /**
     * 删除用户（引用保护：存在 sys_user_role 则拒绝，code=1101）。
     *
     * @param id 用户 ID
     * @return 空载荷成功响应
     */
    @Operation(summary = "删除用户（引用保护）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.okMessage("删除成功");
    }

    /**
     * 批量分配用户角色（全量替换）。
     *
     * @param id 用户 ID
     * @param dto 角色 ID 列表
     * @return 空载荷成功响应
     * @throws BusinessException code=1001 用户不存在；code=1004 角色不存在或已停用
     */
    @Operation(summary = "批量分配用户角色（全量替换）")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody UserRoleAssignDTO dto) {
        userService.assignRoles(id, dto.roleIds());
        return Result.ok(null);
    }
}