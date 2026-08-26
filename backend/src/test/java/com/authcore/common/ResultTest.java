package com.authcore.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统一响应体契约的判定用例（infra/003 AC1）。
 */
class ResultTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Given ok 工厂 When 序列化 Then JSON 恰含 code/message/data 三键且 code=0；
     * Given error 工厂 Then 业务码原样、载荷为空。
     *
     * @throws Exception Jackson 序列化异常
     */
    @Test
    @DisplayName("ok/error 工厂契约与 JSON 三键序列化")
    void test_成功工厂_序列化含codeMessageData且code为0() throws Exception {
        String json = mapper.writeValueAsString(Result.ok("payload"));
        JsonNode node = mapper.readTree(json);
        assertEquals(3, node.size(), "应恰含 code/message/data 三键");
        assertEquals(0, node.get("code").asInt());
        assertTrue(node.has("message"), "应含 message 键");
        assertEquals("payload", node.get("data").asText());

        Result<Void> failure = Result.error(1001, "用户已存在");
        assertEquals(1001, failure.code());
        assertNull(failure.data(), "失败响应载荷应为空");
    }
}
