package com.authcore.service;

import com.authcore.common.BusinessException;
import com.authcore.dto.user.UserCreateDTO;
import com.authcore.dto.user.UserQueryDTO;
import com.authcore.dto.user.UserStatusDTO;
import com.authcore.dto.user.UserUpdateDTO;
import com.authcore.dto.user.UserVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 用户业务服务接口（users/001）。
 */
public interface UserService {

    /**
     * 分页条件查询用户。
     *
     * @param dto 查询参数
     * @return 分页结果，含 list/total/page/size
     */
    IPage<UserVO> queryUsers(UserQueryDTO dto);

    /**
     * 根据 ID 查询用户详情。
     *
     * @param id 用户 ID
     * @return 用户视图对象
     * @throws BusinessException code=1001 用户不存在
     */
    UserVO getUserById(Long id);

    /**
     * 创建用户。
     *
     * @param dto 创建参数
     * @return 创建后的用户视图对象
     * @throws BusinessException code=1002 用户名已存在
     */
    UserVO createUser(UserCreateDTO dto);

    /**
     * 更新用户（仅 nickname、email、phone）。
     *
     * @param id  用户 ID
     * @param dto 更新参数
     * @return 更新后的用户视图对象
     * @throws BusinessException code=1001 用户不存在
     */
    UserVO updateUser(Long id, UserUpdateDTO dto);

    /**
     * 切换用户状态（启用/停用）。
     *
     * @param id     用户 ID
     * @param status 状态值（0=停用，1=启用）
     * @throws BusinessException code=1001 用户不存在；code=400 status 非法
     */
    void toggleStatus(Long id, Integer status);

    /**
     * 删除用户（引用保护：存在 sys_user_role 则拒绝，code=1101）。
     *
     * @param id 用户 ID
     * @throws BusinessException code=1001 用户不存在；code=1101 存在角色引用
     */
    void deleteUser(Long id);

    /**
     * 批量分配用户角色（全量替换）。
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 列表
     * @throws BusinessException code=1001 用户不存在；code=1004 角色不存在或已停用
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 用户自助修改密码。
     *
     * @param userId      用户 ID
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     * @throws BusinessException code=1001 用户不存在；code=1005 旧密码错误；code=1006 新旧密码相同
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 管理员重置用户密码（无需旧密码）。
     *
     * @param userId      目标用户 ID
     * @param newPassword 新密码（明文）
     * @param operatorId  操作者 ID（已由 @PreAuthorize 校验为 ROLE_ADMIN）
     * @throws BusinessException code=1001 用户不存在
     */
    void resetPassword(Long userId, String newPassword, Long operatorId);
}