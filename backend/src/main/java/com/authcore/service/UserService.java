package com.authcore.service;

import com.authcore.common.BusinessException;
import com.authcore.dto.user.UserCreateDTO;
import com.authcore.dto.user.UserQueryDTO;
import com.authcore.dto.user.UserStatusDTO;
import com.authcore.dto.user.UserUpdateDTO;
import com.authcore.dto.user.UserVO;
import com.authcore.entity.SysRole;
import com.authcore.entity.SysUser;
import com.authcore.entity.SysUserRole;
import com.authcore.mapper.SysRoleMapper;
import com.authcore.mapper.SysUserMapper;
import com.authcore.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 用户业务服务（users/001）。
 */
@Service
@Transactional
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(SysUserMapper userMapper,
                       SysUserRoleMapper userRoleMapper,
                       SysRoleMapper roleMapper,
                       PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 分页条件查询用户。
     *
     * @param dto 查询参数
     * @return 分页结果，含 list/total/page/size
     */
    public IPage<UserVO> queryUsers(UserQueryDTO dto) {
        Page<SysUser> page = new Page<>(dto.page(), dto.size());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        if (dto.username() != null && !dto.username().isBlank()) {
            wrapper.like(SysUser::getUsername, dto.username());
        }
        if (dto.nickname() != null && !dto.nickname().isBlank()) {
            wrapper.like(SysUser::getNickname, dto.nickname());
        }
        if (dto.email() != null && !dto.email().isBlank()) {
            wrapper.like(SysUser::getEmail, dto.email());
        }
        if (dto.phone() != null && !dto.phone().isBlank()) {
            wrapper.like(SysUser::getPhone, dto.phone());
        }
        if (dto.status() != null) {
            wrapper.eq(SysUser::getStatus, dto.status());
        }

        wrapper.orderByDesc(SysUser::getCreatedAt);

        IPage<SysUser> resultPage = userMapper.selectPage(page, wrapper);
        return resultPage.convert(this::toVO);
    }

    /**
     * 根据 ID 查询用户详情。
     *
     * @param id 用户 ID
     * @return 用户视图对象
     * @throws BusinessException code=1001 用户不存在
     */
    public UserVO getUserById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }
        return toVO(user);
    }

    /**
     * 创建用户。
     *
     * @param dto 创建参数
     * @return 创建后的用户视图对象
     * @throws BusinessException code=1002 用户名已存在
     */
    public UserVO createUser(UserCreateDTO dto) {
        // 唯一性校验
        if (userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.username())) != null) {
            throw new BusinessException(1002, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.username());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setNickname(dto.nickname());
        user.setEmail(dto.email());
        user.setPhone(dto.phone());
        user.setStatus(1); // 默认启用

        userMapper.insert(user);
        return toVO(user);
    }

    /**
     * 更新用户（仅 nickname、email、phone）。
     *
     * @param id  用户 ID
     * @param dto 更新参数
     * @return 更新后的用户视图对象
     * @throws BusinessException code=1001 用户不存在
     */
    public UserVO updateUser(Long id, UserUpdateDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }

        if (dto.nickname() != null) {
            user.setNickname(dto.nickname());
        }
        if (dto.email() != null) {
            user.setEmail(dto.email());
        }
        if (dto.phone() != null) {
            user.setPhone(dto.phone());
        }
        // username 与 password 不允许通过此接口修改

        userMapper.updateById(user);
        return toVO(user);
    }

    /**
     * 切换用户状态（启用/停用）。
     *
     * @param id     用户 ID
     * @param status 状态值（0=停用，1=启用）
     * @throws BusinessException code=1001 用户不存在；code=400 status 非法
     */
    public void toggleStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "status 必须为 0 或 1");
        }

        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }

        user.setStatus(status);
        userMapper.updateById(user);
    }

    /**
     * 删除用户（引用保护：存在 sys_user_role 则拒绝）。
     *
     * @param id 用户 ID
     * @throws BusinessException code=1001 用户不存在；code=1101 存在角色引用
     */
    public void deleteUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }

        // 检查 sys_user_role 引用
        long refCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, id)
        );
        if (refCount > 0) {
            throw new BusinessException(1101, "用户关联角色，无法删除");
        }

        userMapper.deleteById(id);
    }

    /**
     * 批量分配用户角色（全量替换）。
     *
     * @param userId 用户 ID
     * @param roleIds 角色 ID 列表
     * @throws BusinessException code=1001 用户不存在；code=1004 角色不存在或已停用
     */
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 1. 校验用户存在
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(1001, "用户不存在");
        }

        // 2. 校验角色有效：每个 roleId 在 sys_role 存在且 status=1
        if (roleIds != null && !roleIds.isEmpty()) {
            List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
            if (roles.size() != roleIds.size()) {
                throw new BusinessException(1004, "角色不存在或已停用");
            }
            for (SysRole r : roles) {
                if (r.getStatus() != 1) {
                    throw new BusinessException(1004, "角色已停用");
                }
            }
        }

        // 3. 事务内：先删后增
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long rid : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(rid);
                userRoleMapper.insert(ur);
            }
        }
    }

    /**
     * 用户自助修改密码。
     *
     * @param userId      用户 ID
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     * @throws BusinessException code=1001 用户不存在；code=1005 旧密码错误；code=1006 新旧密码相同
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(1005, "旧密码错误");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(1006, "新密码不能与旧密码相同");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    /**
     * 管理员重置用户密码（无需旧密码）。
     *
     * @param userId      目标用户 ID
     * @param newPassword 新密码（明文）
     * @param operatorId  操作者 ID（已由 @PreAuthorize 校验为 ROLE_ADMIN）
     * @throws BusinessException code=1001 用户不存在
     */
    @Transactional
    public void resetPassword(Long userId, String newPassword, Long operatorId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    private UserVO toVO(SysUser user) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}