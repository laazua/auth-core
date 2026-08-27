package com.authcore.common;

/**
 * 统一响应体（架构 §4）：code 为 int 业务码且 0 表示成功，message 面向调用方，data 为泛型载荷。
 *
 * @param code    业务码：0 成功；资源分段 10xx/11xx/12xx/13xx/14xx；通用错误 400/500
 * @param message 提示信息
 * @param data    载荷，失败时为 null
 * @param <T>     载荷类型
 */
public record Result<T>(int code, String message, T data) {

    /**
     * 构造成功响应。
     *
     * @param data 业务载荷
     * @param <T>  载荷类型
     * @return code=0 的成功 Result
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "success", data);
    }

    /**
     * 构造成功响应（仅消息，无载荷）。
     *
     * @param message 提示信息
     * @param <T>     载荷类型
     * @return code=0、data=null 的成功 Result
     */
    public static <T> Result<T> okMessage(String message) {
        return new Result<>(0, message, null);
    }

    /**
     * 构造失败响应。
     *
     * @param code    业务错误码（取值口径见 docs/01-architecture.md §4）
     * @param message 面向调用方的提示
     * @param <T>     载荷类型
     * @return 非 0 业务码的失败 Result
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
