package com.authcore.service.impl;

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
import com.authcore.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 用户业务服务实现（users/001）。
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(SysUserMapper userMapper,
                           SysUserRoleMapper userRoleMapper,
                           SysRoleMapper roleMapper,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
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

    @Override
    public UserVO getUserById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }
        return toVO(user);
    }

    @Override
    public UserVO createUser(UserCreateDTO dto) {
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
        user.setStatus(1);

        userMapper.insert(user);
        return toVO(user);
    }

    @Override
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

        userMapper.updateById(user);
        return toVO(user);
    }

    @Override
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

    @Override
    public void deleteUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }

        long refCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, id)
        );
        if (refCount > 0) {
            throw new BusinessException(1101, "用户关联角色，无法删除");
        }

        userMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(1001, "用户不存在");
        }

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

    @Override
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

    @Override
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