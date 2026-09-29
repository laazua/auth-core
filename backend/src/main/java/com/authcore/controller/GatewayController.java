package com.authcore.controller;

import com.authcore.common.BusinessException;
import com.authcore.config.security.CustomUserDetails;
import com.authcore.service.GatewayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模块（外部服务）统一访问入口网关（modules/002）。
 *
 * <p>认证由 Security 过滤器完成（未认证/无效 token → 401 + code=1401，无需本层处理）；
 * 本控制器只解析路径并委托 {@link GatewayService} 做模块级准入与转发。
 * 转发成功的响应按代理语义透传下游状态码与响应体（架构 §4 口径登记见 sprint-069 工作单）。
 */
@RestController
public class GatewayController {

    private static final String GATEWAY_PREFIX = "/api/v1/gateway/";

    private final GatewayService gatewayService;

    /**
     * 构造器注入。
     *
     * @param gatewayService 网关准入与转发服务
     */
    public GatewayController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    /**
     * 访问模块（外部服务）：准入通过后转发至模块 base_url，响应原样透传。
     *
     * @param moduleCode 模块编码
     * @param request    原始请求（方法/查询串/请求头/请求体）
     * @return 下游响应透传（状态码与响应体）
     */
    @RequestMapping(GATEWAY_PREFIX + "{moduleCode}/**")
    public ResponseEntity<byte[]> forward(@PathVariable String moduleCode, HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(1401, "未认证");
        }
        return gatewayService.forward(userDetails.getUser().getId(), moduleCode,
                extractSubPath(moduleCode, request), request);
    }

    /**
     * 从请求 URI 中截取模块码之后的剩余路径（不含前导 /）。
     *
     * @param moduleCode 模块编码
     * @param request    原始请求
     * @return 剩余路径，无剩余时为空串
     */
    private String extractSubPath(String moduleCode, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String marker = GATEWAY_PREFIX + moduleCode + "/";
        if (!uri.startsWith(marker) || uri.length() <= marker.length()) {
            return "";
        }
        return uri.substring(marker.length());
    }
}
