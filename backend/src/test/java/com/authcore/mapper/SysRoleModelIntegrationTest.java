package com.authcore.mapper;

import com.authcore.entity.SysRole;
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
 * sys_role 数据层集成判定用例，作用于共享开发库 192.168.165.88/authcore（model/002 AC1-4）。
 *
 * <p>写路径全部 @Transactional 回滚隔离，不在库中残留测试数据；读路径只读。
 */
@SpringBootTest(properties = "spring.flyway.enabled=true")
class SysRoleModelIntegrationTest {

    private static final String IT_PREFIX = "__it_";

    @Autowired
    private SysRoleMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private org.flywaydb.core.Flyway flyway;

    /**
     * 构造可辨识前缀的测试角色。
     *
     * @param codeSuffix 角色编码后缀
     * @return 未持久化的实体
     */
    private SysRole newRole(String codeSuffix) {
        SysRole role = new SysRole();
        role.setName("集成测试角色");
        role.setCode(IT_PREFIX + codeSuffix);
        role.setStatus(1);
        return role;
    }

    /**
     * Given 空缺 V2 When 上下文启动并重放 migrate Then 历史表恰有一条 version=2 成功记录且重放不新增。
     */
    @Test
    @DisplayName("V2 迁移首次应用成功且重放幂等")
    void test_迁移_V2首次应用成功且重放幂等() {
        Integer before = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '2' AND success = 1",
                Integer.class);
        assertEquals(1, before, "V2 应已成功应用一次");

        flyway.migrate();

        Integer after = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '2' AND success = 1",
                Integer.class);
        assertEquals(1, after, "重放后不得产生重复版本记录");
    }

    /**
     * Given V2 已应用 When 查询 information_schema Then 六契约列齐备、name/code 非空约束、code 唯一索引存在。
     */
    @Test
    @DisplayName("sys_role 表结构符合 §3 契约")
    void test_表结构_契约列与唯一键齐备() {
        String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);

        Integer columnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT column_name) FROM information_schema.columns
                WHERE table_schema = ? AND table_name = 'sys_role'
                  AND column_name IN ('id','name','code','status','created_at','updated_at')
                """, Integer.class, schema);
        assertEquals(6, columnCount, "六个契约列必须齐备");

        Integer mandatoryColumns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = ? AND table_name = 'sys_role'
                  AND column_name IN ('name','code') AND is_nullable = 'NO'
                """, Integer.class, schema);
        assertEquals(2, mandatoryColumns, "name 与 code 必须为 NOT NULL（含 Planner 定夺）");

        Integer uniqueIndex = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
                WHERE table_schema = ? AND table_name = 'sys_role'
                  AND index_name = 'uk_sys_role_code' AND non_unique = 0
                """, Integer.class, schema);
        assertEquals(1, uniqueIndex, "code 必须存在唯一索引");
    }

    /**
     * Given 合法角色 When 增查改删 Then 主线可用且 createdAt/updatedAt 自动填充非空。
     */
    @Test
    @Transactional
    @DisplayName("增查改删主线与时间自动填充")
    void test_增查改删主线与时间自动填充() {
        SysRole created = newRole("crud");
        mapper.insert(created);
        assertNotNull(created.getId(), "插入后应回填自增主键");

        SysRole fetched = mapper.selectById(created.getId());
        assertEquals(created.getName(), fetched.getName());
        assertEquals(created.getCode(), fetched.getCode());
        assertEquals(created.getStatus(), fetched.getStatus());
        assertNotNull(fetched.getCreatedAt(), "createdAt 应被自动填充");
        assertNotNull(fetched.getUpdatedAt(), "updatedAt 应被自动填充");

        fetched.setName("改名了");
        mapper.updateById(fetched);
        assertEquals("改名了", mapper.selectById(created.getId()).getName());

        mapper.deleteById(created.getId());
        assertNull(mapper.selectById(created.getId()), "物理删除后不应可查");
    }

    /**
     * Given 同事务插入重复 code When 再次插入 Then 数据库唯一索引触发 DuplicateKeyException。
     */
    @Test
    @Transactional
    @DisplayName("重复 code 触发唯一约束异常")
    void test_重复code_触发唯一约束异常() {
        mapper.insert(newRole("dup"));
        assertThrows(DuplicateKeyException.class, () -> mapper.insert(newRole("dup")));
    }
}
