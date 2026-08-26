package com.authcore.mapper;

import com.authcore.entity.SysUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * sys_user 数据层集成判定用例，作用于共享开发库 192.168.165.88/authcore（model/001 AC1-4）。
 *
 * <p>写路径全部 @Transactional 回滚隔离，不在库中残留测试数据；读路径只读。
 */
@SpringBootTest(properties = "spring.flyway.enabled=true")
class SysUserModelIntegrationTest {

    private static final String IT_PREFIX = "__it_";

    @Autowired
    private SysUserMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private org.flywaydb.core.Flyway flyway;

    /**
     * 构造可辨识前缀的测试用户。
     *
     * @param suffix 用户名后缀
     * @return 未持久化的实体
     */
    private SysUser newUser(String suffix) {
        SysUser user = new SysUser();
        user.setUsername(IT_PREFIX + suffix);
        user.setPassword("$2a$10$itTestOnlyHashValue0000000000000000000000000000");
        user.setNickname("集成测试");
        user.setStatus(1);
        return user;
    }

    /**
     * Given 空缺 V1 When 上下文启动并重放 migrate Then 历史表恰有一条 version=1 成功记录且重放不新增。
     */
    @Test
    @DisplayName("V1 迁移首次应用成功且重放幂等")
    void test_迁移_首次应用成功且重放幂等() {
        Integer before = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1",
                Integer.class);
        assertEquals(1, before, "V1 应已成功应用一次");

        flyway.migrate();

        Integer after = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1",
                Integer.class);
        assertEquals(1, after, "重放后不得产生重复版本记录");
    }

    /**
     * Given V1 已应用 When 查询 information_schema Then 九契约列齐备、password/status 非空约束、username 唯一索引存在。
     */
    @Test
    @DisplayName("sys_user 表结构符合 §3 契约")
    void test_表结构_契约列与唯一键齐备() {
        String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);

        Integer columnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT column_name) FROM information_schema.columns
                WHERE table_schema = ? AND table_name = 'sys_user'
                  AND column_name IN ('id','username','password','nickname','email','phone',
                                      'status','created_at','updated_at')
                """, Integer.class, schema);
        assertEquals(9, columnCount, "九个契约列必须齐备");

        Integer mandatoryColumns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = ? AND table_name = 'sys_user'
                  AND column_name IN ('password','status') AND is_nullable = 'NO'
                """, Integer.class, schema);
        assertEquals(2, mandatoryColumns, "password 与 status 必须为 NOT NULL");

        Integer uniqueIndex = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
                WHERE table_schema = ? AND table_name = 'sys_user'
                  AND index_name = 'uk_sys_user_username' AND non_unique = 0
                """, Integer.class, schema);
        assertEquals(1, uniqueIndex, "username 必须存在唯一索引");
    }

    /**
     * Given 合法用户 When 增查改删 Then 主线可用且 createdAt/updatedAt 自动填充非空。
     */
    @Test
    @Transactional
    @DisplayName("增查改删主线与时间自动填充")
    void test_增查改删主线与时间自动填充() {
        SysUser created = newUser("crud");
        mapper.insert(created);
        assertNotNull(created.getId(), "插入后应回填自增主键");

        SysUser fetched = mapper.selectById(created.getId());
        assertEquals(created.getUsername(), fetched.getUsername());
        assertEquals(created.getPassword(), fetched.getPassword());
        assertEquals(created.getNickname(), fetched.getNickname());
        assertEquals(created.getStatus(), fetched.getStatus());
        assertNotNull(fetched.getCreatedAt(), "createdAt 应被自动填充");
        assertNotNull(fetched.getUpdatedAt(), "updatedAt 应被自动填充");

        fetched.setNickname("改名了");
        mapper.updateById(fetched);
        assertEquals("改名了", mapper.selectById(created.getId()).getNickname());

        mapper.deleteById(created.getId());
        assertNull(mapper.selectById(created.getId()), "物理删除后不应可查");
    }

    /**
     * Given 同事务插入重复 username When 再次插入 Then 数据库唯一索引触发 DuplicateKeyException。
     */
    @Test
    @Transactional
    @DisplayName("重复 username 触发唯一约束异常")
    void test_重复username_触发唯一约束异常() {
        mapper.insert(newUser("dup"));
        assertThrows(DuplicateKeyException.class, () -> mapper.insert(newUser("dup")));
    }
}
