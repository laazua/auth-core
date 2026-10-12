package com.authcore.service;

import com.authcore.dto.module.ModuleQueryDTO;
import com.authcore.dto.permission.PermissionQueryDTO;
import com.authcore.dto.role.RoleQueryDTO;
import com.authcore.dto.user.UserQueryDTO;
import com.authcore.entity.SysModule;
import com.authcore.entity.SysPermission;
import com.authcore.entity.SysRole;
import com.authcore.entity.SysUser;
import com.authcore.mapper.SysModuleMapper;
import com.authcore.mapper.SysPermissionMapper;
import com.authcore.mapper.SysRoleMapper;
import com.authcore.mapper.SysUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 四域列表查询关键词 OR 语义契约（web/044）。
 *
 * <p>前端搜索框对占位符承诺的多字段以同一关键词发送（意图=任一字段命中即返回）；
 * 本类断言后端将非空关键词字段按 OR 组合，且 status/moduleId 等结构化条件保持 AND。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ListSearchOrSemanticsTest {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysModuleMapper moduleMapper;

    @Autowired
    private SysPermissionMapper permissionMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    private SysUser createOrUser(String suffix, int status) {
        SysUser user = new SysUser();
        user.setUsername("zzoruser" + suffix);
        user.setPassword(passwordEncoder.encode("Pass1234"));
        user.setNickname("zzornick" + suffix);
        user.setEmail("zzormail" + suffix + "@example.com");
        user.setPhone("13900000" + String.format("%04d", Math.abs(suffix.hashCode()) % 10000));
        user.setStatus(status);
        userMapper.insert(user);
        return user;
    }

    private SysRole createOrRole(String suffix, int status) {
        SysRole role = new SysRole();
        role.setName("zzor角色" + suffix);
        role.setCode("ZZOR_ROLE_" + suffix);
        role.setStatus(status);
        roleMapper.insert(role);
        return role;
    }

    private SysModule createOrModule(String suffix, int status) {
        SysModule module = new SysModule();
        module.setName("zzor模块" + suffix);
        module.setCode("ZZOR_MOD_" + suffix);
        module.setStatus(status);
        moduleMapper.insert(module);
        return module;
    }

    private SysPermission createOrPermission(String suffix, Long moduleId) {
        SysPermission permission = new SysPermission();
        permission.setModuleId(moduleId);
        permission.setName("zzor权限" + suffix);
        permission.setCode("ZZOR_PERM_" + suffix);
        permissionMapper.insert(permission);
        return permission;
    }

    @Test
    @DisplayName("用户关键词四字段任一命中即返回且与status与逻辑组合")
    void 用户关键词四字段任一命中即返回且与status与逻辑组合() {
        // 准备：用户名可命中、昵称/邮箱/手机号均不含该关键词（AND 语义下必空）
        createOrUser("alpha", 1);
        createOrUser("beta", 0);

        // 关键词=用户名片段，四字段同值发送（前端口径）：username 命中、其余三字段不命中
        var byUsername = userService.queryUsers(new UserQueryDTO(1, 10, "zzoruser", "zzoruser", "zzoruser", "zzoruser", null));
        assertTrue(byUsername.getTotal() >= 2, "四字段同值发送时 username 命中即应返回（OR），实际 total=" + byUsername.getTotal());
        byUsername.getRecords().forEach(vo ->
                assertTrue(vo.username().contains("zzoruser") || vo.nickname().contains("zzoruser")
                                || vo.email().contains("zzoruser") || vo.phone().contains("zzoruser"),
                        "返回行应至少一个关键词字段命中"));

        // 关键词=昵称片段：仅 nickname 命中
        var byNickname = userService.queryUsers(new UserQueryDTO(1, 10, "zzornick", "zzornick", "zzornick", "zzornick", null));
        assertTrue(byNickname.getTotal() >= 2, "nickname 任一命中即应返回，实际 total=" + byNickname.getTotal());

        // status 与关键词 AND 组合：只保留停用行
        var disabled = userService.queryUsers(new UserQueryDTO(1, 10, "zzoruser", "zzoruser", "zzoruser", "zzoruser", 0));
        assertTrue(disabled.getTotal() >= 1, "关键词命中且 status=0 应有结果");
        disabled.getRecords().forEach(vo -> {
            assertEquals(0, vo.status(), "status 组合应保持 AND：返回行全为停用");
            assertTrue(vo.username().contains("zzoruser"), "返回行 username 应命中关键词");
        });

        // 护栏：单字段参数行为不变
        var singleField = userService.queryUsers(new UserQueryDTO(1, 10, "zzoruser", null, null, null, null));
        assertTrue(singleField.getTotal() >= 2, "单字段 username 查询行为应不变");
    }

    @Test
    @DisplayName("角色关键词名称或编码任一命中且与status与逻辑组合")
    void 角色关键词名称或编码任一命中且与status与逻辑组合() {
        // 准备：名称含中文后缀、编码含大写前缀——单侧关键词只命中一侧字段
        createOrRole("A", 1);
        createOrRole("B", 0);

        // 关键词=编码片段：code 命中、name 不命中（AND 语义下必空）
        var byCode = roleService.queryRoles(new RoleQueryDTO(1, 10, "ZZOR_ROLE", "ZZOR_ROLE", null));
        assertTrue(byCode.getTotal() >= 2, "name/code 同值发送时 code 命中即应返回（OR），实际 total=" + byCode.getTotal());
        byCode.getRecords().forEach(vo ->
                assertTrue(vo.name().contains("ZZOR_ROLE") || vo.code().contains("ZZOR_ROLE"),
                        "返回行应 name 或 code 命中"));

        // 关键词=名称片段：仅 name 命中
        var byName = roleService.queryRoles(new RoleQueryDTO(1, 10, "zzor角色", "zzor角色", null));
        assertTrue(byName.getTotal() >= 2, "name 任一命中即应返回，实际 total=" + byName.getTotal());

        // status 与关键词 AND 组合
        var enabled = roleService.queryRoles(new RoleQueryDTO(1, 10, "ZZOR_ROLE", "ZZOR_ROLE", 1));
        assertTrue(enabled.getTotal() >= 1, "关键词命中且 status=1 应有结果");
        enabled.getRecords().forEach(vo -> {
            assertEquals(1, vo.status(), "status 组合应保持 AND：返回行全为启用");
            assertTrue(vo.code().contains("ZZOR_ROLE"), "返回行 code 应命中关键词");
        });
    }

    @Test
    @DisplayName("权限关键词名称或编码任一命中且与模块筛选与逻辑组合")
    void 权限关键词名称或编码任一命中且与模块筛选与逻辑组合() {
        // 准备：甲模块下 2 条权限、乙模块下 1 条权限，名称/编码单侧可命中
        SysModule moduleA = createOrModule("A", 1);
        SysModule moduleB = createOrModule("B", 1);
        createOrPermission("A1", moduleA.getId());
        createOrPermission("A2", moduleA.getId());
        createOrPermission("B1", moduleB.getId());

        // 关键词=名称片段：name 命中、code 不命中（AND 语义下必空）
        var byName = permissionService.queryPermissions(new PermissionQueryDTO(1, 10, "zzor权限", "zzor权限", null));
        assertTrue(byName.getTotal() >= 3, "name/code 同值发送时 name 命中即应返回（OR），实际 total=" + byName.getTotal());

        // 关键词=编码片段：仅 code 命中
        var byCode = permissionService.queryPermissions(new PermissionQueryDTO(1, 10, "ZZOR_PERM", "ZZOR_PERM", null));
        assertTrue(byCode.getTotal() >= 3, "code 任一命中即应返回，实际 total=" + byCode.getTotal());

        // moduleId 与关键词 AND 组合：限定甲模块后仅甲模块下权限
        var scoped = permissionService.queryPermissions(
                new PermissionQueryDTO(1, 10, "zzor权限", "zzor权限", moduleA.getId()));
        assertTrue(scoped.getTotal() >= 2, "关键词命中且 moduleId=甲应有结果");
        scoped.getRecords().forEach(vo ->
                assertEquals(moduleA.getId(), vo.moduleId(), "moduleId 组合应保持 AND：返回行全属甲模块"));

        // moduleId 不匹配时保持 AND：0 结果
        var noMatch = permissionService.queryPermissions(
                new PermissionQueryDTO(1, 10, "zzor权限", "zzor权限", 999999999L));
        assertEquals(0, noMatch.getTotal(), "moduleId 不匹配时关键词组合应返回 0");
    }

    @Test
    @DisplayName("模块关键词名称或编码任一命中且与status与逻辑组合")
    void 模块关键词名称或编码任一命中且与status与逻辑组合() {
        // 准备：甲启用、乙停用
        createOrModule("甲", 1);
        createOrModule("乙", 0);

        // 关键词=编码片段：code 命中、name 不命中（AND 语义下必空）
        var byCode = moduleService.queryModules(new ModuleQueryDTO(1, 10, "ZZOR_MOD", "ZZOR_MOD", null));
        assertTrue(byCode.getTotal() >= 2, "name/code 同值发送时 code 命中即应返回（OR），实际 total=" + byCode.getTotal());
        byCode.getRecords().forEach(vo ->
                assertTrue(vo.name().contains("ZZOR_MOD") || vo.code().contains("ZZOR_MOD"),
                        "返回行应 name 或 code 命中"));

        // 关键词=名称片段：仅 name 命中
        var byName = moduleService.queryModules(new ModuleQueryDTO(1, 10, "zzor模块", "zzor模块", null));
        assertTrue(byName.getTotal() >= 2, "name 任一命中即应返回，实际 total=" + byName.getTotal());

        // status 与关键词 AND 组合
        var enabled = moduleService.queryModules(new ModuleQueryDTO(1, 10, "ZZOR_MOD", "ZZOR_MOD", 1));
        assertTrue(enabled.getTotal() >= 1, "关键词命中且 status=1 应有结果");
        enabled.getRecords().forEach(vo ->
                assertEquals(1, vo.status(), "status 组合应保持 AND：返回行全为启用"));

        var disabled = moduleService.queryModules(new ModuleQueryDTO(1, 10, "ZZOR_MOD", "ZZOR_MOD", 0));
        assertTrue(disabled.getTotal() >= 1, "关键词命中且 status=0 应有结果");
        disabled.getRecords().forEach(vo ->
                assertEquals(0, vo.status(), "status 组合应保持 AND：返回行全为停用"));
    }
}
