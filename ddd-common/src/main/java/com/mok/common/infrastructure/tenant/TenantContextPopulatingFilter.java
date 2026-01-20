package com.mok.common.infrastructure.tenant;

import com.mok.common.application.exception.BizException;
import com.mok.common.infrastructure.common.Const;
import com.mok.common.infrastructure.security.TokenProvider;
import com.mok.common.infrastructure.security.TokenSessionDTO;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import java.util.Optional;

@Order(Ordered.LOWEST_PRECEDENCE)
@Filter("/**")
public class TenantContextPopulatingFilter implements HttpServerFilter {

    private final SecurityService securityService;
    private final TokenProvider tokenProvider;

    public TenantContextPopulatingFilter(SecurityService securityService, TokenProvider tokenProvider) {
        this.securityService = securityService;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        if (request.getPath().startsWith("/public") || request.getPath().startsWith("/api/auth/login")) {
            return chain.proceed(request);
        }

        Optional<Authentication> authOpt = securityService.getAuthentication();
        String tenantId = null;
        String username = null;
        Long userId = null;

        if (authOpt.isPresent()) {
            Authentication auth = authOpt.get();
            tenantId = (String) auth.getAttributes().get("tenantId");
            username = auth.getName();
            Object userIdObj = auth.getAttributes().get("userId");
            userId = userIdObj != null ? Long.valueOf(userIdObj.toString()) : null;
        } else {
            String token = request.getHeaders().getAuthorization()
                    .map(a -> a.replace("Bearer ", ""))
                    .orElse(null);
            if (token != null) {
                TokenSessionDTO session = tokenProvider.getSession(token);
                if (session != null) {
                    tenantId = session.getTenantId();
                    username = session.getUsername();
                    userId = session.getPrincipal() != null ? session.getPrincipal().userId() : null;
                }
            }
        }

        if (tenantId == null) {
            tenantId = request.getHeaders().get("X-Tenant-Id");
        }

        if (tenantId == null || tenantId.isBlank()) {
            throw new BizException("无法获取租户信息");
        }

        String finalTenantId = tenantId;
        String finalUsername = username != null ? username : "";
        Long finalUserId = userId != null ? userId : 0L;

        return Mono.defer(() ->
                Mono.from(chain.proceed(request))
                        .contextWrite(ctx -> ctx
                                .put(Const.TENANT_ID, finalTenantId)
                                .put(Const.USERNAME, finalUsername)
                                .put(Const.USER_ID, finalUserId)
                        )
        ).transform(mono ->
                ScopedValue.where(TenantContextHolder.TENANT_ID, finalTenantId)
                        .where(TenantContextHolder.USERNAME, finalUsername)
                        .where(TenantContextHolder.USER_ID, finalUserId)
                        .call(() -> mono)
        );
    }
}