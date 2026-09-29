package com.authcore.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

/**
 * 模块（外部服务）统一访问入口服务接口（modules/002）：模块级准入裁决 + 按 base_url 转发。
 */
public interface GatewayService {

    /**
     * 对已认证用户做模块级准入裁决，通过后把请求转发到模块服务地址并透传下游响应。
     *
     * @param userId     当前登录用户 ID（来自 SecurityContext，不信任请求体）
     * @param moduleCode 模块编码（网关路径变量）
     * @param subPath    模块服务地址之后的剩余路径（不含前导 /，可为空串）
     * @param request    原始 HTTP 请求（方法、查询串、请求头、请求体来源）
     * @return 下游响应状态码与响应体的透传结果
     */
    ResponseEntity<byte[]> forward(Long userId, String moduleCode, String subPath, HttpServletRequest request);
}
