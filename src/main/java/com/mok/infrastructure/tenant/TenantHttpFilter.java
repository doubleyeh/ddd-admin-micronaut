package com.mok.infrastructure.tenant;

import com.mok.infrastructure.common.Const;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Filter("/**")
public class TenantHttpFilter implements HttpServerFilter {
    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String tenantId = Optional.ofNullable(request.getHeaders().get("X-Tenant-Id"))
                .orElse(Const.SUPER_TENANT_ID);

        return Mono.defer(() ->
                ScopedValue.where(TenantContext.TENANT_ID, tenantId)
                        .call(() -> Mono.from(chain.proceed(request)))
        );
    }
}