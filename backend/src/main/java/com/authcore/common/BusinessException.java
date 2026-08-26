package com.authcore.common;

/**
 * 业务失败异常：code 必须取自架构 §4 的资源分段，禁止自造码段（规范第 5 节）。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    /**
     * @param code    业务错误码（docs/01-architecture.md §4 分段）
     * @param message 面向调用方的提示
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * @return 业务错误码
     */
    public int getCode() {
        return code;
    }
}
