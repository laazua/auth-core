package com.authcore.test.unit;

import com.authcore.util.JsonTestUtil;
import com.authcore.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单元测试示例：不启动容器，无真实库，纯逻辑验证，极速执行。
 * 对应目录：src/test/java/com/authcore/test/unit/
 */
class UnitTestExampleSpec {

    @Test
    @DisplayName("单元测试：TestDataBuilder 纯内存构建，无数据库")
    void userBuilderPureMemory() {
        var user = TestDataBuilder.user()
            .withUsername("unit_test")
            .withEmail("unit@test.com")
            .build();

        assertThat(user.getUsername()).isEqualTo("unit_test");
        assertThat(user.getEmail()).isEqualTo("unit@test.com");
    }

    @Test
    @DisplayName("单元测试：JsonTestUtil 纯内存序列化")
    void jsonUtilPureMemory() {
        String json = JsonTestUtil.toJson(new Object() {
            public final String field = "value";
        });
        assertThat(json).contains("value");
    }
}