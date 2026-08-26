package com.authcore.mapper;

import com.authcore.entity.SysModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * sys_module 数据层集成判定用例，作用于共享开发库 192.168.165.88/authcore（model/003 AC1-4）。
 *
 * <p>写路径全部 @Transactional 回滚隔离，不在库中残留测试数据；读路径只读。
 */
class SysModuleModelIntegrationTest extends AbstractModelIntegrationTest {

    @Autowired
    private SysModuleMapper mapper;

    /**
     * 构造可辨识前缀的测试模块。
     *
     * @param codeSuffix 模块编码后缀
     * @return 未持久化的实体
     */
    private SysModule newModule(String codeSuffix) {
        SysModule module = new SysModule();
        module.setName("集成测试模块");
        module.setCode(IT_PREFIX + codeSuffix);
        module.setBaseUrl("http://it.local/" + codeSuffix);
        module.setDescription("集成测试描述");
        module.setStatus(1);
        return module;
    }

    /**
     * Given 空缺 V3 When 上下文启动并重放 migrate Then 历史表恰有一条 version=3 成功记录且重放不新增。
     */
    @Test
    @DisplayName("V3 迁移首次应用成功且重放幂等")
    void test_迁移_V3首次应用成功且重放幂等() {
        assertEquals(1, countFlywaySuccess("3"), "V3 应已成功应用一次");

        flyway.migrate();

        assertEquals(1, countFlywaySuccess("3"), "重放后不得产生重复版本记录");
    }

    /**
     * Given V3 已应用 When 查询 information_schema Then 八契约列齐备、必填/可空分组正确、code 唯一索引存在。
     */
    @Test
    @DisplayName("sys_module 表结构符合 §3 契约")
    void test_表结构_契约列与约束齐备() {
        int notNullColumns = countColumnsWithNullability("sys_module", "NO",
                "id", "name", "code", "status", "created_at", "updated_at");
        int nullableColumns = countColumnsWithNullability("sys_module", "YES",
                "base_url", "description");
        assertEquals(6, notNullColumns, "六列必须为 NOT NULL（含 Planner 定夺）");
        assertEquals(2, nullableColumns, "base_url 与 description 必须可空");
        assertEquals(1, countUniqueIndexes("sys_module", "uk_sys_module_code"),
                "code 必须存在唯一索引");
    }

    /**
     * Given 合法模块 When 增查改删 Then 主线可用且 createdAt/updatedAt 自动填充非空。
     */
    @Test
    @Transactional
    @DisplayName("增查改删主线与时间自动填充")
    void test_增查改删主线与时间自动填充() {
        SysModule created = newModule("crud");
        mapper.insert(created);
        assertNotNull(created.getId(), "插入后应回填自增主键");

        SysModule fetched = mapper.selectById(created.getId());
        assertEquals(created.getName(), fetched.getName());
        assertEquals(created.getCode(), fetched.getCode());
        assertEquals(created.getBaseUrl(), fetched.getBaseUrl());
        assertEquals(created.getDescription(), fetched.getDescription());
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
        mapper.insert(newModule("dup"));
        assertThrows(DuplicateKeyException.class, () -> mapper.insert(newModule("dup")));
    }
}
