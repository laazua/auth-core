package com.authcore.service;

import com.authcore.dto.user.UserCreateDTO;
import com.authcore.dto.user.UserQueryDTO;
import com.authcore.dto.user.UserStatusDTO;
import com.authcore.dto.user.UserUpdateDTO;
import com.authcore.dto.user.UserVO;
import com.authcore.entity.SysUser;
import com.authcore.entity.SysUserRole;
import com.authcore.mapper.SysUserMapper;
import com.authcore.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UserService 业务逻辑判定用例（users/001）。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    private static final String TEST_PREFIX = "__ut_user_";

    private SysUser createTestUser(String suffix, int status) {
        SysUser user = new SysUser();
        user.setUsername(TEST_PREFIX + suffix);
        user.setPassword(passwordEncoder.encode("Pass1234"));
        user.setNickname("测试用户" + suffix);
        user.setEmail("test" + suffix + "@example.com");
        user.setPhone("1390000" + String.format("%04d", suffix.hashCode() & 0xFFFF));
        user.setStatus(status);
        userMapper.insert(user);
        return user;
    }

    /**
     * 分页查询：条件构造器正确、分页参数生效。
     */
    @Test
    @DisplayName("分页查询条件构造器与分页参数生效")
    void queryUsersWithConditionsAndPagination() {
        // 准备测试数据
        createTestUser("query1", 1);
        createTestUser("query2", 1);
        createTestUser("query3", 0); // 停用用户

        UserQueryDTO query = new UserQueryDTO(1, 10, "query", null, null, null, 1);
        var page = userService.queryUsers(query);

        assertEquals(1, page.getCurrent(), "当前页应为 1");
        assertEquals(10, page.getSize(), "每页大小应为 10");
        assertEquals(2, page.getTotal(), "总数应为 2（仅 status=1 且 username 含 query）");
        assertEquals(2, page.getRecords().size(), "记录数应为 2");

        // 验证每条记录字段完整
        for (UserVO vo : page.getRecords()) {
            assertNotNull(vo.id(), "id 不应为空");
            assertNotNull(vo.username(), "username 不应为空");
            assertNotNull(vo.nickname(), "nickname 不应为空");
            assertNotNull(vo.email(), "email 不应为空");
            assertNotNull(vo.phone(), "phone 不应为空");
            assertNotNull(vo.status(), "status 不应为空");
            assertTrue(vo.username().contains("query"), "username 应包含查询关键词");
            assertEquals(1, vo.status(), "status 应为 1");
        }
    }

    /**
     * 单用户详情：存在返回 VO，不存在抛 1001。
     */
    @Test
    @DisplayName("单用户详情存在返回 VO 不存在抛 1001")
    void getUserByIdExistsAndNotFound() {
        SysUser user = createTestUser("detail", 1);

        // 存在
        UserVO vo = userService.getUserById(user.getId());
        assertEquals(user.getId(), vo.id());
        assertEquals(user.getUsername(), vo.username());
        assertEquals(user.getNickname(), vo.nickname());
        assertEquals(user.getEmail(), vo.email());
        assertEquals(user.getPhone(), vo.phone());
        assertEquals(user.getStatus(), vo.status());

        // 不存在
        var ex = assertThrows(com.authcore.common.BusinessException.class,
                () -> userService.getUserById(99999L));
        assertEquals(1001, ex.getCode());
    }

    /**
     * 创建用户：密码 BCrypt 加密、username 唯一校验（code=1002）。
     */
    @Test
    @DisplayName("创建用户密码 BCrypt 加密且用户名唯一")
    void createUserEncryptsPasswordAndUniqueUsername() {
        UserCreateDTO dto = new UserCreateDTO(
                TEST_PREFIX + "create",
                "Pass1234",
                "创建测试",
                "create@test.com",
                "13900000005"
        );

        UserVO vo = userService.createUser(dto);
        assertNotNull(vo.id(), "id 不应为空");
        assertEquals(dto.username(), vo.username());
        assertEquals(dto.nickname(), vo.nickname());
        assertEquals(dto.email(), vo.email());
        assertEquals(dto.phone(), vo.phone());
        assertEquals(1, vo.status(), "默认状态应为 1");

        // 验证数据库中密码为 BCrypt
        SysUser saved = userMapper.selectById(vo.id());
        assertNotNull(saved.getPassword(), "密码不应为空");
        assertTrue(saved.getPassword().startsWith("$2a$10$"), "密码应为 BCrypt 哈希");
        assertTrue(passwordEncoder.matches("Pass1234", saved.getPassword()), "明文密码应匹配 BCrypt 哈希");

        // 重复 username 应抛 1002
        var ex = assertThrows(com.authcore.common.BusinessException.class,
                () -> userService.createUser(dto));
        assertEquals(1002, ex.getCode());
    }

    /**
     * 更新用户：nickname/email/phone 更新、username/password 不变。
     */
    @Test
    @DisplayName("更新用户仅 nickname/email/phone、username 与 password 不变")
    void updateUserOnlyNicknameEmailPhone() {
        SysUser user = createTestUser("update", 1);
        String originalPassword = user.getPassword();
        String originalUsername = user.getUsername();

        UserUpdateDTO dto = new UserUpdateDTO("新昵称", "new@test.com", "13900000006");
        UserVO vo = userService.updateUser(user.getId(), dto);

        assertEquals("新昵称", vo.nickname());
        assertEquals("new@test.com", vo.email());
        assertEquals("13900000006", vo.phone());
        assertEquals(originalUsername, vo.username(), "username 不应改变");

        // 验证数据库 password 未变
        SysUser updated = userMapper.selectById(user.getId());
        assertEquals(originalPassword, updated.getPassword(), "password 不应改变");
    }

    /**
     * 启停用：status 仅 0/1、更新生效、停用用户不可登录（间接验证）。
     */
    @Test
    @DisplayName("启停用 status 仅 0/1 且更新生效")
    void toggleStatusValidValuesAndEffective() {
        SysUser user = createTestUser("status", 1);

        // 停用
        userService.toggleStatus(user.getId(), 0);
        SysUser disabled = userMapper.selectById(user.getId());
        assertEquals(0, disabled.getStatus(), "status 应更新为 0");

        // 启用
        userService.toggleStatus(user.getId(), 1);
        SysUser enabled = userMapper.selectById(user.getId());
        assertEquals(1, enabled.getStatus(), "status 应更新为 1");

        // 非法 status 应抛 BusinessException(code=400)
        var ex = assertThrows(com.authcore.common.BusinessException.class,
                () -> userService.toggleStatus(user.getId(), 2));
        assertEquals(400, ex.getCode(), "非法 status 应返回 code=400");
        assertTrue(ex.getMessage().contains("status"), "异常信息应包含 status");
    }

    /**
     * 删除用户：无引用物理删除、有引用抛 1101。
     */
    @Test
    @DisplayName("删除用户无引用物理删除 有引用抛 1101")
    void deleteUserReferenceProtection() {
        // 无引用用户
        SysUser user1 = createTestUser("delete1", 1);
        userService.deleteUser(user1.getId());
        assertNull(userMapper.selectById(user1.getId()), "无引用用户应物理删除");

        // 有引用用户：插入 sys_user_role
        SysUser user2 = createTestUser("delete2", 1);
        SysUserRole ur = new SysUserRole();
        ur.setUserId(user2.getId());
        ur.setRoleId(2L); // ROLE_USER
        userRoleMapper.insert(ur);

        var ex = assertThrows(com.authcore.common.BusinessException.class,
                () -> userService.deleteUser(user2.getId()));
        assertEquals(1101, ex.getCode(), "有角色引用应抛 1101");
        assertNotNull(userMapper.selectById(user2.getId()), "有引用用户不应被删除");
    }
}