package com.authcore.service;

import com.authcore.dto.auth.MeResponse;
import com.authcore.dto.auth.RoleVO;
import com.authcore.dto.auth.UserVO;

import java.util.List;

/**
 * 认证业务服务接口：提供当前用户信息聚合查询（auth/004）。
 */
public interface AuthService {

    /**
     * 获取当前用户完整信息：用户基本信息 + 角色列表 + 权限码集合（去重）。
     *
     * @param userId 用户 ID
     * @return MeResponse 聚合响应
     */
    MeResponse getCurrentUserInfo(Long userId);

    /**
     * 校验用户是否拥有指定权限（auth/005）。
     *
     * @param userId         用户 ID
     * @param permissionCode 权限编码
     * @return true 表示拥有权限，false 表示无权限或用户不存在
     */
    boolean checkPermission(Long userId, String permissionCode);
}