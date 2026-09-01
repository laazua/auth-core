package com.authcore.util;

import com.authcore.entity.SysUser;
import com.authcore.entity.SysRole;
import com.authcore.entity.SysModule;
import com.authcore.entity.SysPermission;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 测试数据构建器：Builder 模式快速构建测试实体。
 * 用于集成测试中创建符合约束的测试数据。
 */
public final class TestDataBuilder {

    private TestDataBuilder() {}

    // ==================== SysUser ====================

    public static UserBuilder user() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private String username = "test_user_" + System.nanoTime();
        private String password = "Test@123456";
        private String nickname = "测试用户";
        private String email = "test@example.com";
        private String phone = "13800138000";
        private Integer status = 1;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();

        public UserBuilder withUsername(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder withNickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public UserBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder withPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public UserBuilder withStatus(Integer status) {
            this.status = status;
            return this;
        }

        public UserBuilder withCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserBuilder withUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserBuilder customize(Consumer<SysUser> customizer) {
            customizer.accept(buildInternal());
            return this;
        }

        public SysUser build() {
            SysUser user = buildInternal();
            return user;
        }

        private SysUser buildInternal() {
            SysUser user = new SysUser();
            user.setUsername(username);
            user.setPassword(password);
            user.setNickname(nickname);
            user.setEmail(email);
            user.setPhone(phone);
            user.setStatus(status);
            user.setCreatedAt(createdAt);
            user.setUpdatedAt(updatedAt);
            return user;
        }
    }

    // ==================== SysRole ====================

    public static RoleBuilder role() {
        return new RoleBuilder();
    }

    public static class RoleBuilder {
        private String name = "测试角色";
        private String code = "test_role_" + System.nanoTime();
        private Integer status = 1;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();

        public RoleBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public RoleBuilder withCode(String code) {
            this.code = code;
            return this;
        }

        public RoleBuilder withStatus(Integer status) {
            this.status = status;
            return this;
        }

        public RoleBuilder customize(Consumer<SysRole> customizer) {
            customizer.accept(buildInternal());
            return this;
        }

        public SysRole build() {
            SysRole role = buildInternal();
            return role;
        }

        private SysRole buildInternal() {
            SysRole role = new SysRole();
            role.setName(name);
            role.setCode(code);
            role.setStatus(status);
            role.setCreatedAt(createdAt);
            role.setUpdatedAt(updatedAt);
            return role;
        }
    }

    // ==================== SysModule ====================

    public static ModuleBuilder module() {
        return new ModuleBuilder();
    }

    public static class ModuleBuilder {
        private String name = "测试模块";
        private String code = "test_module_" + System.nanoTime();
        private String baseUrl = "http://localhost:8080";
        private String description = "测试模块描述";
        private Integer status = 1;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();

        public ModuleBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public ModuleBuilder withCode(String code) {
            this.code = code;
            return this;
        }

        public ModuleBuilder withBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public ModuleBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public ModuleBuilder withStatus(Integer status) {
            this.status = status;
            return this;
        }

        public ModuleBuilder customize(Consumer<SysModule> customizer) {
            customizer.accept(buildInternal());
            return this;
        }

        public SysModule build() {
            SysModule module = buildInternal();
            return module;
        }

        private SysModule buildInternal() {
            SysModule module = new SysModule();
            module.setName(name);
            module.setCode(code);
            module.setBaseUrl(baseUrl);
            module.setDescription(description);
            module.setStatus(status);
            module.setCreatedAt(createdAt);
            module.setUpdatedAt(updatedAt);
            return module;
        }
    }

    // ==================== SysPermission ====================

    public static PermissionBuilder permission() {
        return new PermissionBuilder();
    }

    public static class PermissionBuilder {
        private Long moduleId = 1L;
        private String name = "测试权限";
        private String code = "test:permission:" + System.nanoTime();
        private String description = "测试权限描述";
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();

        public PermissionBuilder withModuleId(Long moduleId) {
            this.moduleId = moduleId;
            return this;
        }

        public PermissionBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public PermissionBuilder withCode(String code) {
            this.code = code;
            return this;
        }

        public PermissionBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public PermissionBuilder customize(Consumer<SysPermission> customizer) {
            customizer.accept(buildInternal());
            return this;
        }

        public SysPermission build() {
            SysPermission permission = buildInternal();
            return permission;
        }

        private SysPermission buildInternal() {
            SysPermission permission = new SysPermission();
            permission.setModuleId(moduleId);
            permission.setName(name);
            permission.setCode(code);
            permission.setDescription(description);
            permission.setCreatedAt(createdAt);
            permission.setUpdatedAt(updatedAt);
            return permission;
        }
    }

    // ==================== 批量构建辅助 ====================

    public static List<SysUser> users(int count, Consumer<UserBuilder> customizer) {
        List<SysUser> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UserBuilder builder = user().withUsername("batch_user_" + i);
            customizer.accept(builder);
            list.add(builder.build());
        }
        return list;
    }

    public static List<SysRole> roles(int count, Consumer<RoleBuilder> customizer) {
        List<SysRole> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            RoleBuilder builder = role().withCode("batch_role_" + i);
            customizer.accept(builder);
            list.add(builder.build());
        }
        return list;
    }
}