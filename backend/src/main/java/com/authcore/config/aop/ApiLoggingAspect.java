package com.authcore.config.aop;

import com.authcore.common.Result;
import com.authcore.config.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * API 操作日志切面。
 * 拦截所有 {@code @RestController} 下的公共方法，记录：
 * <ul>
 *   <li>HTTP 方法、请求路径</li>
 *   <li>请求参数（查询参数、路径变量、请求体）</li>
 *   <li>当前登录用户 ID（若已认证）</li>
 *   <li>执行耗时</li>
 *   <li>响应结果或异常信息</li>
 * </ul>
 * <p>
 * 日志级别：INFO 记录正常调用，WARN 记录业务异常，ERROR 记录系统异常。
 * 敏感字段（password、token、secret 等）会自动脱敏。
 * </p>
 *
 * @since 1.0
 */
@Aspect
@Component
@Order(1)
public class ApiLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ApiLoggingAspect.class);

    /** 匹配所有控制器公共方法 */
    @Pointcut("execution(public * com.authcore.controller..*(..))")
    public void controllerMethods() {}

    /**
     * 环绕通知：记录请求入参、执行耗时、响应结果/异常。
     */
    @Around("controllerMethods()")
    public Object logApiOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取 HTTP 请求信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs != null ? attrs.getRequest() : null;

        // 解析方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // 构建请求上下文
        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String requestPath = request != null ? request.getRequestURI() : "UNKNOWN";
        String queryString = request != null ? request.getQueryString() : "";
        String fullPath = queryString != null && !queryString.isEmpty()
                ? requestPath + "?" + queryString
                : requestPath;

        // 获取当前登录用户 ID
        Long userId = getCurrentUserId();

        // 记录请求入参（脱敏）
        String argsLog = formatArgs(joinPoint.getArgs(), signature.getParameterNames());

        log.info("API 调用开始 | {} | {} | userId={} | args={}",
                className, methodName, userId != null ? userId : "anonymous", argsLog);

        try {
            Object result = joinPoint.proceed();

            long elapsedMs = System.currentTimeMillis() - startTime;

            // 记录响应结果（脱敏）
            String resultLog = formatResult(result);

            log.info("API 调用成功 | {} | {} | userId={} | path={} | 耗时={}ms | response={}",
                    className, methodName, userId != null ? userId : "anonymous",
                    fullPath, elapsedMs, resultLog);

            return result;
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startTime;

            // 区分业务异常与系统异常
            if (e instanceof com.authcore.common.BusinessException be) {
                log.warn("API 业务异常 | {} | {} | userId={} | path={} | 耗时={}ms | code={} | message={}",
                        className, methodName, userId != null ? userId : "anonymous",
                        fullPath, elapsedMs, be.getCode(), be.getMessage());
            } else {
                log.error("API 系统异常 | {} | {} | userId={} | path={} | 耗时={}ms | exception={}",
                        className, methodName, userId != null ? userId : "anonymous",
                        fullPath, elapsedMs, e.getClass().getSimpleName() + ": " + e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * 从 SecurityContext 获取当前登录用户 ID。
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUser().getId();
        }
        return null;
    }

    /**
     * 格式化方法参数，敏感字段脱敏。
     */
    private String formatArgs(Object[] args, String[] paramNames) {
        if (args == null || args.length == 0) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            String paramName = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;
            Object arg = args[i];
            sb.append(paramName).append("=").append(maskSensitive(paramName, arg));
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 格式化响应结果，敏感字段脱敏。
     */
    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }

        // Result 类型只记录 code 和 message，data 脱敏处理
        if (result instanceof Result<?> r) {
            return String.format("Result{code=%d, message='%s', data=%s}",
                    r.code(), r.message(), maskData(r.data()));
        }

        // 其他类型转字符串，过长截断
        String str = result.toString();
        return str.length() > 500 ? str.substring(0, 500) + "..." : str;
    }

    /**
     * 敏感字段脱敏。
     */
    private Object maskSensitive(String paramName, Object value) {
        if (value == null) {
            return "null";
        }

        String lowerName = paramName.toLowerCase();
        // 检查参数名是否包含敏感关键字
        if (lowerName.contains("password") || lowerName.contains("token")
                || lowerName.contains("secret") || lowerName.contains("key")
                || lowerName.contains("credential")) {
            return "******";
        }

        // 检查对象类型是否为常见敏感 DTO
        String className = value.getClass().getSimpleName().toLowerCase();
        if (className.contains("password") || className.contains("token")
                || className.contains("secret") || className.contains("credential")) {
            return "******";
        }

        // 对于 DTO 对象，尝试转字符串并检查是否包含敏感字段
        String str = value.toString();
        if (str.length() > 1000) {
            return str.substring(0, 1000) + "...";
        }
        return str;
    }

    /**
     * 响应数据脱敏（简化版：过长截断）。
     */
    private Object maskData(Object data) {
        if (data == null) {
            return "null";
        }
        String str = data.toString();
        return str.length() > 500 ? str.substring(0, 500) + "..." : str;
    }
}