package com.authcore.mapper;

import com.authcore.entity.SysPermission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * sys_permission 数据层集成判定用例，作用于共享开发库 192.168.165.88/authcore（model/004 AC1-4）。
 *
 * <p>写路径全部 @Transactional 回滚隔离，不在库中残留测试数据；读路径只读。
 * 注意 §3 契约：本表七列、无 status 列；module_id NOT NULL 为 §6.1「权限必须归属模块」的数据层表达。
 */
@ActiveProfiles("test")
class SysPermissionModelIntegrationTest extends AbstractModelIntegrationTest {

    @Autowired
    private SysPermissionMapper mapper;

    /**
     * 构造可辨识前缀的测试权限。
     *
     * @param codeSuffix 权限编码后缀
     * @return 未持久化的实体
     */
    private SysPermission newPermission(String codeSuffix) {
        SysPermission permission = new SysPermission();
        permission.setModuleId(1L);
        permission.setName("集成测试权限");
        permission.setCode(IT_PREFIX + codeSuffix);
        permission.setDescription("集成测试描述");
        return permission;
    }

    /**
     * Given 空缺 V4 When 上下文启动并重放 migrate Then 历史表恰有一条 version=4 成功记录且重放不新增。
     */
    @Test
    @DisplayName("V4 迁移首次应用成功且重放幂等")
    void test_迁移_V4首次应用成功且重放幂等() {
        assertEquals(1, countFlywaySuccess("4"), "V4 应已成功应用一次");

        flyway.migrate();

        assertEquals(1, countFlywaySuccess("4"), "重放后不得产生重复版本记录");
    }

    /**
     * Given V4 已应用 When 查询 information_schema Then 七契约列齐备、NOT NULL 分组含 module_id、description 可空、code 唯一索引存在。
     */
    @Test
    @DisplayName("sys_permission 表结构符合 §3 契约")
    void test_表结构_契约列与约束齐备() {
        int notNullColumns = countColumnsWithNullability("sys_permission", "NO",
                "id", "module_id", "name", "code", "created_at", "updated_at");
        int nullableColumns = countColumnsWithNullability("sys_permission", "YES", "description");
        assertEquals(6, notNullColumns, "六列必须为 NOT NULL（module_id 承载 §6.1）");
        assertEquals(1, nullableColumns, "仅 description 可空");
        assertEquals(0, countColumnsWithNullability("sys_permission", "NO", "status"),
                "契约无 status 列，不得擅自增加");
        assertEquals(1, countUniqueIndexes("sys_permission", "uk_sys_permission_code"),
                "code 必须存在唯一索引");
    }

    /**
     * Given 合法权限 When 增查改删 Then 主线可用且 createdAt/updatedAt 自动填充非空。
     */
    @Test
    @Transactional
    @DisplayName("增查改删主线与时间自动填充")
    void test_增查改删主线与时间自动填充() {
        SysPermission created = newPermission("crud");
        mapper.insert(created);
        assertNotNull(created.getId(), "插入后应回填自增主键");

        SysPermission fetched = mapper.selectById(created.getId());
        assertEquals(created.getModuleId(), fetched.getModuleId());
        assertEquals(created.getName(), fetched.getName());
        assertEquals(created.getCode(), fetched.getCode());
        assertEquals(created.getDescription(), fetched.getDescription());
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
        mapper.insert(newPermission("dup"));
        assertThrows(DuplicateKeyException.class, () -> mapper.insert(newPermission("dup")));
    }
}
