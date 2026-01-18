package com.mok.infrastructure.tenant;

import com.mok.application.exception.BizException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TenantContextPopulatingFilterTest {

    private SecurityService securityService;
    private TenantContextPopulatingFilter filter;
    private HttpRequest<?> request;
    private ServerFilterChain chain;

    @BeforeEach
    void setUp() {
        securityService = mock(SecurityService.class);
        filter = new TenantContextPopulatingFilter(securityService);
        request = mock(HttpRequest.class);
        chain = mock(ServerFilterChain.class);

        MutableHttpResponse<?> response = mock(MutableHttpResponse.class);
        when(chain.proceed(any())).thenReturn(Mono.just(response));
    }

    @Test
    void doFilter_ExcludesPublicPath() {
        when(request.getPath()).thenReturn("/public/test");

        Publisher<MutableHttpResponse<?>> result = filter.doFilter(request, chain);

        assertNotNull(result);
        verify(chain).proceed(request);
        verify(securityService, never()).getAuthentication();
    }

    @Test
    void doFilter_ExcludesLoginPath() {
        when(request.getPath()).thenReturn("/api/auth/login");

        Publisher<MutableHttpResponse<?>> result = filter.doFilter(request, chain);

        assertNotNull(result);
        verify(chain).proceed(request);
        verify(securityService, never()).getAuthentication();
    }

    @Test
    void doFilter_AuthenticatedUser() {
        when(request.getPath()).thenReturn("/api/test");
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenantId", "tenant1");
        attributes.put("userId", 123L);
        when(auth.getAttributes()).thenReturn(attributes);
        when(securityService.getAuthentication()).thenReturn(Optional.of(auth));

        Publisher<MutableHttpResponse<?>> result = filter.doFilter(request, chain);

        assertNotNull(result);
        verify(chain).proceed(request);
    }

    @Test
    void doFilter_AnonymousUser_WithTenantHeader() {
        when(request.getPath()).thenReturn("/api/test");
        when(securityService.getAuthentication()).thenReturn(Optional.empty());
        when(request.getHeaders()).thenReturn(mock(io.micronaut.http.HttpHeaders.class));
        when(request.getHeaders().get("X-Tenant-Id")).thenReturn("tenant1");

        Publisher<MutableHttpResponse<?>> result = filter.doFilter(request, chain);

        assertNotNull(result);
        verify(chain).proceed(request);
    }

    @Test
    void doFilter_NoTenantId_ThrowsBizException() {
        when(request.getPath()).thenReturn("/api/test");
        when(securityService.getAuthentication()).thenReturn(Optional.empty());
        when(request.getHeaders()).thenReturn(mock(io.micronaut.http.HttpHeaders.class));
        when(request.getHeaders().get("X-Tenant-Id")).thenReturn(null);

        assertThrows(BizException.class, () -> filter.doFilter(request, chain));
    }

    @Test
    void doFilter_BlankTenantId_ThrowsBizException() {
        when(request.getPath()).thenReturn("/api/test");
        when(securityService.getAuthentication()).thenReturn(Optional.empty());
        when(request.getHeaders()).thenReturn(mock(io.micronaut.http.HttpHeaders.class));
        when(request.getHeaders().get("X-Tenant-Id")).thenReturn("   ");

        assertThrows(BizException.class, () -> filter.doFilter(request, chain));
    }
}