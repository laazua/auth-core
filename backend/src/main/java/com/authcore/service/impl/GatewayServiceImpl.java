package com.authcore.service.impl;

import com.authcore.common.BusinessException;
import com.authcore.entity.SysModule;
import com.authcore.entity.SysPermission;
import com.authcore.mapper.SysModuleMapper;
import com.authcore.mapper.SysPermissionMapper;
import com.authcore.service.AuthService;
import com.authcore.service.GatewayService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 模块（外部服务）统一访问入口服务实现（modules/002）。
 *
 * <p>不标注 {@code @Transactional}：准入均为单语句只读查询，且方法内含下游 HTTP 调用，
 * 数据库事务不得横跨网络等待。
 *
 * <p>准入语义（架构 §6.1 权限必须归属模块、§6.5 停用模块下权限视为无效）：
 * 模块存在（否则 1304→400）、base_url 已配置（否则 1305→400）、模块启用且
 * 当前用户持有其下至少一个权限（否则 AccessDeniedException→403+1403）。
 */
@Service
public class GatewayServiceImpl implements GatewayService {

    private static final Logger log = LoggerFactory.getLogger(GatewayServiceImpl.class);

    private static final Integer MODULE_ENABLED = 1;

    /** 转发时剥离的请求头（认证凭据 Authorization/Cookie 与逐跳头不外泄），全小写比较。 */
    private static final Set<String> STRIPPED_HEADERS = Set.of(
            "authorization", "cookie", "host", "content-length", "connection", "keep-alive", "transfer-encoding", "expect");

    private final SysModuleMapper moduleMapper;
    private final SysPermissionMapper permissionMapper;
    private final AuthService authService;
    private final RestTemplate restTemplate;

    /**
     * 构造器注入。
     *
     * @param moduleMapper    模块数据访问
     * @param permissionMapper 权限数据访问
     * @param authService     当前用户权限聚合（auth/004）
     * @param restTemplate    下游转发客户端
     */
    public GatewayServiceImpl(SysModuleMapper moduleMapper,
                              SysPermissionMapper permissionMapper,
                              AuthService authService,
                              RestTemplate restTemplate) {
        this.moduleMapper = moduleMapper;
        this.permissionMapper = permissionMapper;
        this.authService = authService;
        this.restTemplate = restTemplate;
    }

    @Override
    public ResponseEntity<byte[]> forward(Long userId, String moduleCode, String subPath,
                                          HttpServletRequest request) {
        SysModule module = resolveModule(moduleCode);
        checkBaseUrl(module);
        checkAllowed(userId, module);
        return doForward(module, subPath, request);
    }

    private SysModule resolveModule(String moduleCode) {
        SysModule module = moduleMapper.selectOne(new LambdaQueryWrapper<SysModule>()
                .eq(SysModule::getCode, moduleCode));
        if (module == null) {
            throw new BusinessException(1304, "模块不存在");
        }
        return module;
    }

    private void checkBaseUrl(SysModule module) {
        if (module.getBaseUrl() == null || module.getBaseUrl().isBlank()) {
            throw new BusinessException(1305, "模块服务地址未配置");
        }
    }

    private void checkAllowed(Long userId, SysModule module) {
        if (!MODULE_ENABLED.equals(module.getStatus())) {
            log.debug("网关准入拒绝：模块停用, code={}", module.getCode());
            throw new AccessDeniedException("无权访问该模块");
        }
        List<String> modulePermissions = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getModuleId, module.getId())).stream()
                .map(SysPermission::getCode)
                .toList();
        Set<String> userPermissions = new HashSet<>(authService.getCurrentUserInfo(userId).permissions());
        if (modulePermissions.stream().noneMatch(userPermissions::contains)) {
            log.debug("网关准入拒绝：用户无模块有效权限, userId={}, code={}", userId, module.getCode());
            throw new AccessDeniedException("无权访问该模块");
        }
    }

    private ResponseEntity<byte[]> doForward(SysModule module, String subPath, HttpServletRequest request) {
        URI target = buildTargetUri(module.getBaseUrl(), subPath, request.getQueryString());
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        RequestEntity<byte[]> entity = RequestEntity.method(method, target)
                .headers(buildForwardHeaders(request))
                .body(readBody(request));
        ResponseEntity<byte[]> downstream = restTemplate.exchange(entity, byte[].class);

        HttpHeaders responseHeaders = new HttpHeaders();
        if (downstream.getHeaders().getContentType() != null) {
            responseHeaders.setContentType(downstream.getHeaders().getContentType());
        }
        // web/038a AC4：预置 SAMEORIGIN 覆盖 Spring Security 默认 DENY（框架对已存在同名头跳过），
        // 使同源 iframe 可内嵌、外源仍被拒；下游的 XFO/CSP 因仅复制 Content-Type 而自然不透传
        responseHeaders.set("X-Frame-Options", "SAMEORIGIN");
        log.debug("网关转发完成, target={}, status={}", target, downstream.getStatusCode().value());
        return ResponseEntity.status(downstream.getStatusCode())
                .headers(responseHeaders)
                .body(downstream.getBody());
    }

    private URI buildTargetUri(String baseUrl, String subPath, String queryString) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = subPath == null || subPath.isEmpty()
                ? ""
                : (subPath.startsWith("/") ? subPath : "/" + subPath);
        String query = queryString == null || queryString.isEmpty() ? "" : "?" + queryString;
        return URI.create(base + path + query);
    }

    private HttpHeaders buildForwardHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (STRIPPED_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                headers.add(name, values.nextElement());
            }
        }
        return headers;
    }

    private byte[] readBody(HttpServletRequest request) {
        try {
            byte[] bytes = request.getInputStream().readAllBytes();
            return bytes.length == 0 ? null : bytes;
        } catch (IOException e) {
            throw new UncheckedIOException("读取转发请求体失败", e);
        }
    }
}
