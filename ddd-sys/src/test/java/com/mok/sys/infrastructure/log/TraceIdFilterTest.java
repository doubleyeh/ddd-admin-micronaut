package com.mok.sys.infrastructure.log;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.filter.ServerFilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.Mockito.*;

class TraceIdFilterTest {

    private TraceIdFilter filter;
    private HttpRequest<?> request;
    private ServerFilterChain chain;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        filter = new TraceIdFilter();
        request = mock(HttpRequest.class);
        chain = mock(ServerFilterChain.class);
        headers = mock(HttpHeaders.class);

        MutableHttpResponse<?> response = mock(MutableHttpResponse.class);
        when(chain.proceed(request)).thenReturn(Mono.just(response));
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void doFilter_WithTraceIdHeader_UsesHeaderValue() {
        String traceId = "custom-trace-id";
        when(request.getHeaders()).thenReturn(headers);
        when(headers.get("X-Trace-ID")).thenReturn(traceId);

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            ((Mono<?>) filter.doFilter(request, chain)).block();

            verify(chain).proceed(request);
            mdcMock.verify(() -> MDC.put("traceId", traceId));
            mdcMock.verify(() -> MDC.remove("traceId"));
        }
    }

    @Test
    void doFilter_WithoutTraceIdHeader_GeneratesNewId() {
        when(request.getHeaders()).thenReturn(headers);
        when(headers.get("X-Trace-ID")).thenReturn(null);

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class);
             MockedStatic<UUID> uuidMock = mockStatic(UUID.class)) {
            UUID mockUUID = mock(UUID.class);
            when(mockUUID.toString()).thenReturn("12345678-1234-1234-1234-123456789012");
            uuidMock.when(UUID::randomUUID).thenReturn(mockUUID);

            ((Mono<?>) filter.doFilter(request, chain)).block();

            verify(chain).proceed(request);
            mdcMock.verify(() -> MDC.put("traceId", "12345678123412341234123456789012"));
            mdcMock.verify(() -> MDC.remove("traceId"));
        }
    }

    @Test
    void doFilter_WithEmptyTraceIdHeader_GeneratesNewId() {
        when(request.getHeaders()).thenReturn(headers);
        when(headers.get("X-Trace-ID")).thenReturn("");

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class);
             MockedStatic<UUID> uuidMock = mockStatic(UUID.class)) {
            UUID mockUUID = mock(UUID.class);
            when(mockUUID.toString()).thenReturn("12345678-1234-1234-1234-123456789012");
            uuidMock.when(UUID::randomUUID).thenReturn(mockUUID);

            ((Mono<?>) filter.doFilter(request, chain)).block();

            verify(chain).proceed(request);
            mdcMock.verify(() -> MDC.put("traceId", "12345678123412341234123456789012"));
            mdcMock.verify(() -> MDC.remove("traceId"));
        }
    }
}
