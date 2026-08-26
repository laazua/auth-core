package com.authcore.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * MyBatis-Plus 分页插件装配的判定用例（infra/002 AC2）。
 */
class MybatisPlusConfigTest {

    /**
     * Given 配置类 When 获取拦截器 Then 分页拦截器已注册且方言为 MySQL。
     */
    @Test
    @DisplayName("分页拦截器已注册且方言为 MySQL")
    void test_分页拦截器_已注册且方言为MySQL() {
        MybatisPlusInterceptor interceptor = new MybatisPlusConfig().mybatisPlusInterceptor();

        assertNotNull(interceptor, "MybatisPlusInterceptor 未装配");
        PaginationInnerInterceptor pagination =
                assertInstanceOf(PaginationInnerInterceptor.class,
                        interceptor.getInterceptors().get(0));
        assertEquals(DbType.MYSQL, pagination.getDbType());
    }
}
