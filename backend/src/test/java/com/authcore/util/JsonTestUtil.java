package com.authcore.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * JSON 测试工具：统一 ObjectMapper 配置，提供序列化/反序列化/断言辅助。
 */
public final class JsonTestUtil {

    private static final ObjectMapper MAPPER = createMapper();

    private JsonTestUtil() {}

    /**
     * 创建统一配置的 ObjectMapper：
     * - 失败在未知属性：false（宽容多余字段）
     * - 日期格式：ISO-8601
     * - 空值不序列化
     */
    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        return mapper;
    }

    /**
     * 获取共享 ObjectMapper 实例（线程安全）。
     */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * 对象序列化为 JSON 字符串。
     */
    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * JSON 字符串反序列化为指定类型。
     */
    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * JSON 字符串反序列化为泛型类型（如 List<T>、Map<String, T>、Result<T>）。
     */
    public static <T> T fromJson(String json, com.fasterxml.jackson.core.type.TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 断言两个对象 JSON 相等（忽略字段顺序、空值差异）。
     */
    public static void assertJsonEquals(Object expected, Object actual) {
        String expectedJson = toJson(expected);
        String actualJson = toJson(actual);
        if (!expectedJson.equals(actualJson)) {
            throw new AssertionError("JSON 不相等\n期望: " + expectedJson + "\n实际: " + actualJson);
        }
    }

    /**
     * 断言 JSON 字符串包含指定键值对（宽松匹配）。
     */
    public static void assertJsonContains(String json, String key, Object value) {
        String expectedFragment = "\"" + key + "\":" + toJson(value);
        if (!json.contains(expectedFragment)) {
            throw new AssertionError("JSON 不包含片段: " + expectedFragment + "\n完整 JSON: " + json);
        }
    }

    /**
     * 格式化 JSON 字符串（用于调试输出）。
     */
    public static String prettyPrint(String json) {
        try {
            Object obj = MAPPER.readValue(json, Object.class);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return json; // 解析失败返回原串
        }
    }

    /**
     * 将对象转为格式化 JSON 字符串（用于调试输出）。
     */
    public static String prettyPrint(Object value) {
        return prettyPrint(toJson(value));
    }
}