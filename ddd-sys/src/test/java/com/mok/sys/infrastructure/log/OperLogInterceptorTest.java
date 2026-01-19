package com.mok.sys.infrastructure.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mok.sys.domain.model.OperLog;
import com.mok.common.infrastructure.tenant.TenantContextHolder;
import com.mok.common.infrastructure.util.SysUtil;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.context.ServerRequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OperLogInterceptorTest {

    private ApplicationEventPublisher<OperLogEvent> eventPublisher;
    private ObjectMapper objectMapper;
    private OperLogInterceptor interceptor;
    private MockedStatic<TenantContextHolder> tenantContextHolderMock;
    private MockedStatic<ServerRequestContext> serverRequestContextMock;
    private MockedStatic<SysUtil> sysUtilMock;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ApplicationEventPublisher.class);
        objectMapper = new ObjectMapper();
        interceptor = new OperLogInterceptor(eventPublisher, objectMapper);

        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        serverRequestContextMock = mockStatic(ServerRequestContext.class);
        sysUtilMock = mockStatic(SysUtil.class);

        tenantContextHolderMock.when(TenantContextHolder::getUsername).thenReturn("testUser");
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn("tenant1");
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
        serverRequestContextMock.close();
        sysUtilMock.close();
    }

    @Test
    void intercept_SuccessfulMethodExecution() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        Object result = "success";

        when(context.proceed()).thenReturn(result);
        when(context.findAnnotation(OperLogRecord.class)).thenReturn(Optional.of(mock(AnnotationValue.class)));
        when(context.getTarget()).thenReturn(this);
        when(context.getExecutableMethod()).thenReturn(mock(ExecutableMethod.class));

        Object interceptedResult = interceptor.intercept(context);

        assertEquals(result, interceptedResult);
        verify(eventPublisher).publishEvent(any(OperLogEvent.class));
    }

    @Test
    void intercept_MethodThrowsException() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        RuntimeException exception = new RuntimeException("test error");

        when(context.proceed()).thenThrow(exception);
        when(context.findAnnotation(OperLogRecord.class)).thenReturn(Optional.of(mock(AnnotationValue.class)));
        when(context.getTarget()).thenReturn(this);
        when(context.getExecutableMethod()).thenReturn(mock(ExecutableMethod.class));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> interceptor.intercept(context));
        assertEquals(exception, thrown);
        verify(eventPublisher).publishEvent(any(OperLogEvent.class));
    }

    @Test
    void intercept_NoAnnotation_DoesNotLog() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        Object result = "success";

        when(context.proceed()).thenReturn(result);
        when(context.findAnnotation(OperLogRecord.class)).thenReturn(Optional.empty());

        Object interceptedResult = interceptor.intercept(context);

        assertEquals(result, interceptedResult);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void intercept_ContextNull() throws Exception {
        assertThrows(NullPointerException.class, () -> interceptor.intercept(null));
    }

    @Test
    void handleLog_WithRequest() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        AnnotationValue<OperLogRecord> annotation = mock(AnnotationValue.class);
        HttpRequest<?> request = mock(HttpRequest.class);

        when(annotation.get("title", String.class)).thenReturn(Optional.of("Test Title"));
        when(annotation.enumValue("businessType", BusinessType.class)).thenReturn(Optional.of(BusinessType.INSERT));
        when(annotation.isTrue("isSaveRequestData")).thenReturn(true);
        when(annotation.isTrue("isSaveResponseData")).thenReturn(true);

        when(context.getTarget()).thenReturn(this);
        when(context.getExecutableMethod()).thenReturn(mock(ExecutableMethod.class));
        when(context.getParameterValues()).thenReturn(new Object[]{"param"});

        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.of(request));
        when(request.getPath()).thenReturn("/test");
        when(request.getMethodName()).thenReturn("GET");
        sysUtilMock.when(() -> SysUtil.getIpAddress(request)).thenReturn("127.0.0.1");

        assertDoesNotThrow(() -> interceptor.handleLog(context, annotation, null, "result", 100L));
        verify(eventPublisher).publishEvent(any(OperLogEvent.class));
    }

    @Test
    void handleLog_WithoutRequest() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        AnnotationValue<OperLogRecord> annotation = mock(AnnotationValue.class);

        when(annotation.get("title", String.class)).thenReturn(Optional.of("Test Title"));
        when(annotation.enumValue("businessType", BusinessType.class)).thenReturn(Optional.of(BusinessType.INSERT));
        when(annotation.isTrue("isSaveRequestData")).thenReturn(false);
        when(annotation.isTrue("isSaveResponseData")).thenReturn(false);

        when(context.getTarget()).thenReturn(this);
        when(context.getExecutableMethod()).thenReturn(mock(ExecutableMethod.class));

        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> interceptor.handleLog(context, annotation, null, "result", 100L));
        verify(eventPublisher).publishEvent(any(OperLogEvent.class));
    }

    @Test
    void handleLog_WithException() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        AnnotationValue<OperLogRecord> annotation = mock(AnnotationValue.class);
        Exception exception = new RuntimeException("test error");

        when(annotation.get("title", String.class)).thenReturn(Optional.of("Test Title"));
        when(annotation.enumValue("businessType", BusinessType.class)).thenReturn(Optional.of(BusinessType.INSERT));
        when(annotation.isTrue("isSaveRequestData")).thenReturn(false);

        when(context.getTarget()).thenReturn(this);
        when(context.getExecutableMethod()).thenReturn(mock(ExecutableMethod.class));

        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> interceptor.handleLog(context, annotation, exception, "result", 100L));
        verify(eventPublisher).publishEvent(any(OperLogEvent.class));
    }

    @Test
    void handleLog_ObjectMapperThrowsException() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        AnnotationValue<OperLogRecord> annotation = mock(AnnotationValue.class);
        Object badResult = new Object() { public String toString() { throw new RuntimeException("bad"); } };
        HttpRequest<?> request = mock(HttpRequest.class);

        when(annotation.get("title", String.class)).thenReturn(Optional.of("Test Title"));
        when(annotation.enumValue("businessType", BusinessType.class)).thenReturn(Optional.of(BusinessType.INSERT));
        when(annotation.isTrue("isSaveRequestData")).thenReturn(true);
        when(annotation.isTrue("isSaveResponseData")).thenReturn(true);

        when(context.getTarget()).thenReturn(this);
        when(context.getExecutableMethod()).thenReturn(mock(ExecutableMethod.class));
        when(context.getParameterValues()).thenReturn(new Object[]{});

        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.of(request));
        when(request.getPath()).thenReturn("/test");
        when(request.getMethodName()).thenReturn("GET");
        sysUtilMock.when(() -> SysUtil.getIpAddress(request)).thenReturn("127.0.0.1");

        assertDoesNotThrow(() -> interceptor.handleLog(context, annotation, null, badResult, 100L));
        verify(eventPublisher).publishEvent(any(OperLogEvent.class));
    }

    @Test
    void handleLog_PublishEventThrowsException() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        AnnotationValue<OperLogRecord> annotation = mock(AnnotationValue.class);
        HttpRequest<?> request = mock(HttpRequest.class);

        when(annotation.get("title", String.class)).thenReturn(Optional.of("Test Title"));
        when(annotation.enumValue("businessType", BusinessType.class)).thenReturn(Optional.of(BusinessType.INSERT));
        when(annotation.isTrue("isSaveRequestData")).thenReturn(false);
        when(annotation.isTrue("isSaveResponseData")).thenReturn(false);

        when(context.getTarget()).thenReturn(this);
        when(context.getExecutableMethod()).thenReturn(mock(ExecutableMethod.class));

        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.of(request));
        when(request.getPath()).thenReturn("/test");
        when(request.getMethodName()).thenReturn("GET");
        sysUtilMock.when(() -> SysUtil.getIpAddress(request)).thenReturn("127.0.0.1");

        doThrow(new RuntimeException("publish failed")).when(eventPublisher).publishEvent(any(OperLogEvent.class));

        assertDoesNotThrow(() -> interceptor.handleLog(context, annotation, null, "result", 100L));
        verify(eventPublisher).publishEvent(any(OperLogEvent.class));
    }

    @Test
    void handleLog_WithResponseDataNull() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        AnnotationValue<OperLogRecord> annotation = mock(AnnotationValue.class);
        HttpRequest<?> request = mock(HttpRequest.class);

        when(annotation.get("title", String.class)).thenReturn(Optional.of("Test Title"));
        when(annotation.enumValue("businessType", BusinessType.class)).thenReturn(Optional.of(BusinessType.INSERT));
        when(annotation.isTrue("isSaveRequestData")).thenReturn(false);
        when(annotation.isTrue("isSaveResponseData")).thenReturn(true);

        when(context.getTarget()).thenReturn(this);
        when(context.getExecutableMethod()).thenReturn(mock(ExecutableMethod.class));

        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.of(request));
        when(request.getPath()).thenReturn("/test");
        when(request.getMethodName()).thenReturn("GET");
        sysUtilMock.when(() -> SysUtil.getIpAddress(request)).thenReturn("127.0.0.1");

        assertDoesNotThrow(() -> interceptor.handleLog(context, annotation, null, null, 100L));
        verify(eventPublisher).publishEvent(any(OperLogEvent.class));
    }

    @Test
    void handleLog_WithResponseDataNotNull() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        AnnotationValue<OperLogRecord> annotation = mock(AnnotationValue.class);
        HttpRequest<?> request = mock(HttpRequest.class);

        when(annotation.get("title", String.class)).thenReturn(Optional.of("Test Title"));
        when(annotation.enumValue("businessType", BusinessType.class)).thenReturn(Optional.of(BusinessType.INSERT));
        when(annotation.isTrue("isSaveRequestData")).thenReturn(false);
        when(annotation.isTrue("isSaveResponseData")).thenReturn(true);

        when(context.getTarget()).thenReturn(this);
        when(context.getExecutableMethod()).thenReturn(mock(ExecutableMethod.class));

        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.of(request));
        when(request.getPath()).thenReturn("/test");
        when(request.getMethodName()).thenReturn("GET");
        sysUtilMock.when(() -> SysUtil.getIpAddress(request)).thenReturn("127.0.0.1");

        assertDoesNotThrow(() -> interceptor.handleLog(context, annotation, null, "result", 100L));
        verify(eventPublisher).publishEvent(any(OperLogEvent.class));
    }

    @Test
    void handleLog_WithRequestDataOnly() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        AnnotationValue<OperLogRecord> annotation = mock(AnnotationValue.class);
        HttpRequest<?> request = mock(HttpRequest.class);

        when(annotation.get("title", String.class)).thenReturn(Optional.of("Test Title"));
        when(annotation.enumValue("businessType", BusinessType.class)).thenReturn(Optional.of(BusinessType.INSERT));
        when(annotation.isTrue("isSaveRequestData")).thenReturn(true);
        when(annotation.isTrue("isSaveResponseData")).thenReturn(false);

        when(context.getTarget()).thenReturn(this);
        when(context.getExecutableMethod()).thenReturn(mock(ExecutableMethod.class));
        when(context.getParameterValues()).thenReturn(new Object[]{"param"});

        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.of(request));
        when(request.getPath()).thenReturn("/test");
        when(request.getMethodName()).thenReturn("GET");
        sysUtilMock.when(() -> SysUtil.getIpAddress(request)).thenReturn("127.0.0.1");

        assertDoesNotThrow(() -> interceptor.handleLog(context, annotation, null, "result", 100L));
        verify(eventPublisher).publishEvent(any(OperLogEvent.class));
    }

    @Test
    void getRequestValue_WithArgs() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        when(context.getParameterValues()).thenReturn(new Object[]{"arg1", "arg2"});

        java.lang.reflect.Method method = interceptor.getClass().getDeclaredMethod("getRequestValue", MethodInvocationContext.class);
        method.setAccessible(true);
        String result = (String) method.invoke(interceptor, context);

        assertNotNull(result);
        assertTrue(result.contains("arg1"));
    }

    @Test
    void getRequestValue_WithoutArgs() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        when(context.getParameterValues()).thenReturn(null);

        java.lang.reflect.Method method = interceptor.getClass().getDeclaredMethod("getRequestValue", MethodInvocationContext.class);
        method.setAccessible(true);
        String result = (String) method.invoke(interceptor, context);

        assertNull(result);
    }

    @Test
    void getRequestValue_WithLongParams() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        String longParam = "a".repeat(3000);
        when(context.getParameterValues()).thenReturn(new Object[]{longParam});

        java.lang.reflect.Method method = interceptor.getClass().getDeclaredMethod("getRequestValue", MethodInvocationContext.class);
        method.setAccessible(true);
        String result = (String) method.invoke(interceptor, context);

        assertNotNull(result);
        assertTrue(result.endsWith("..."));
    }

    @Test
    void getRequestValue_SerializationFails() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        Object badObject = new Object() { public String toString() { throw new RuntimeException("bad"); } };
        when(context.getParameterValues()).thenReturn(new Object[]{badObject});

        java.lang.reflect.Method method = interceptor.getClass().getDeclaredMethod("getRequestValue", MethodInvocationContext.class);
        method.setAccessible(true);
        String result = (String) method.invoke(interceptor, context);

        assertEquals("error serializing args", result);
    }
}