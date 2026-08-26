package com.authcore.mapper;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * model 层集成测试公共基座：承载真实库上下文装配与 information_schema / flyway 历史断言辅助，
 * 消除各表测试类的同构样板（承接 sprint-004/005 评审建议）。
 */
@SpringBootTest(properties = "spring.flyway.enabled=true")
abstract class AbstractModelIntegrationTest {

    /** 测试数据统一前缀，便于共享库残留识别与清理核查。 */
    protected static final String IT_PREFIX = "__it_";

    /** 真实库访问句柄，供子类做只读结构断言。 */
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /** Flyway 句柄，供重放幂等断言使用。 */
    @Autowired
    protected Flyway flyway;

    /**
     * 统计指定版本的成功迁移记录数。
     *
     * @param version Flyway 版本号
     * @return 记录数
     */
    protected int countFlywaySuccess(String version) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = ? AND success = 1",
                Integer.class, version);
        return count == null ? 0 : count;
    }

    /**
     * 按 is_nullable 口径统计表中命中指定列名集合的数量。
     *
     * @param table            表名
     * @param expectedNullable 期望空性："NO" 或 "YES"
     * @param columns          列名集合
     * @return 命中列数
     */
    protected int countColumnsWithNullability(String table, String expectedNullable, String... columns) {
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            placeholders.append(i == 0 ? "?" : ", ?");
        }
        Object[] args = new Object[columns.length + 2];
        args[0] = table;
        args[1] = expectedNullable;
        System.arraycopy(columns, 0, args, 2, columns.length);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? "
                        + "AND is_nullable = ? AND column_name IN (" + placeholders + ")",
                Integer.class, args);
        return count == null ? 0 : count;
    }

    /**
     * 统计表中指定名称的唯一索引数量。
     *
     * @param table     表名
     * @param indexName 索引名
     * @return 唯一索引数量
     */
    protected int countUniqueIndexes(String table, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = ? "
                        + "AND index_name = ? AND non_unique = 0",
                Integer.class, table, indexName);
        return count == null ? 0 : count;
    }
}
