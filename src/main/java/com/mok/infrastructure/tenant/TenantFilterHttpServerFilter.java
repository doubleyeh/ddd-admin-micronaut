package com.mok.infrastructure.tenant;

import com.mok.infrastructure.common.Const;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

@Filter("/**")
public class TenantFilterHttpServerFilter implements HttpServerFilter {

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String tenantId = request.getHeaders().get("X-Tenant-Id");
        if (tenantId == null) {
            tenantId = Const.SUPER_TENANT_ID;
        }
        return ScopedValue.where(TenantContext.TENANT_ID, tenantId)
                .call(() -> Flux.from(chain.proceed(request)));
    }
}