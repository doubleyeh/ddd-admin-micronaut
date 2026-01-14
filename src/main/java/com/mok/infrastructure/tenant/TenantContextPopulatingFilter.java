package com.mok.infrastructure.tenant;

import com.mok.application.exception.BizException;
import io.micronaut.core.annotation.Order;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.security.utils.SecurityService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Order(-100)
@Filter("/**")
public class TenantContextPopulatingFilter implements HttpServerFilter {

    private final SecurityService securityService;

    public TenantContextPopulatingFilter(SecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        // 排除不需要租户信息的路径
        if (request.getPath().startsWith("/public") || request.getPath().startsWith("/api/auth/login")) {
            return chain.proceed(request);
        }

        Optional<io.micronaut.security.authentication.Authentication> authOpt = securityService.getAuthentication();

        String tenantId;
        String username = null;
        Long userId = null;

        if (authOpt.isPresent()) {
            // 已认证用户: 从安全上下文中获取信息
            io.micronaut.security.authentication.Authentication auth = authOpt.get();
            tenantId = (String) auth.getAttributes().get("tenantId");
            username = auth.getName();
            userId = (Long) auth.getAttributes().get("userId");
        } else {
            // 匿名用户: 从请求头获取信息
            tenantId = request.getHeaders().get("X-Tenant-Id");
        }

        if (tenantId == null || tenantId.isBlank()) {
            throw new BizException("无法获取租户信息");
        }

        // 绑定所有上下文值并继续执行过滤器链
        String finalTenantId = tenantId;
        String finalUsername = username;
        Long finalUserId = userId;

        return ScopedValue.where(TenantContextHolder.TENANT_ID, finalTenantId)
                .where(TenantContextHolder.USERNAME, finalUsername)
                .where(TenantContextHolder.USER_ID, finalUserId)
                .call(() -> Mono.from(chain.proceed(request)));
    }
}
