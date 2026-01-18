package com.mok.infrastructure.security;

import com.mok.application.sys.dto.auth.LoginRequest;
import com.mok.application.sys.service.LoginLogService;
import com.mok.domain.sys.model.LoginLog;
import com.mok.infrastructure.util.SysUtil;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.event.LoginFailedEvent;
import io.micronaut.security.event.LoginSuccessfulEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthenticationEventListenerTest {

    private LoginLogService loginLogService;
    private AuthenticationEventListener listener;
    private MockedStatic<ServerRequestContext> serverRequestContextMock;
    private MockedStatic<SysUtil> sysUtilMock;

    @BeforeEach
    void setUp() {
        loginLogService = mock(LoginLogService.class);
        listener = new AuthenticationEventListener(loginLogService);
        serverRequestContextMock = mockStatic(ServerRequestContext.class);
        sysUtilMock = mockStatic(SysUtil.class);
    }

    @AfterEach
    void tearDown() {
        serverRequestContextMock.close();
        sysUtilMock.close();
    }

    @Test
    void onApplicationEvent_LoginSuccessfulEvent_HandlesSuccess() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("testuser");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenantId", "tenant1");
        when(authentication.getAttributes()).thenReturn(attributes);

        LoginSuccessfulEvent event = new LoginSuccessfulEvent(authentication);

        sysUtilMock.when(() -> SysUtil.getIpAddress(any())).thenReturn("127.0.0.1");

        listener.onApplicationEvent(event);

        verify(loginLogService).createLoginLog(any(LoginLog.class));
    }

    @Test
    void onApplicationEvent_LoginFailedEvent_HandlesFailure() {
        AuthenticationResponse response = AuthenticationResponse.failure("Invalid credentials");
        LoginFailedEvent event = new LoginFailedEvent(response);

        HttpRequest<Object> request = mock(HttpRequest.class);
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("testuser");
        loginReq.setTenantId("tenant1");

        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.of(request));
        when(request.getBody(LoginRequest.class)).thenReturn(Optional.of(loginReq));
        sysUtilMock.when(() -> SysUtil.getIpAddress(any())).thenReturn("127.0.0.1");

        listener.onApplicationEvent(event);

        verify(loginLogService).createLoginLog(any(LoginLog.class));
    }

    @Test
    void onApplicationEvent_OtherEvent_DoesNothing() {
        Object event = new Object();

        listener.onApplicationEvent(event);

        verifyNoInteractions(loginLogService);
    }

    @Test
    void handleLoginSuccess_CreatesLoginLog() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("testuser");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenantId", "tenant1");
        when(authentication.getAttributes()).thenReturn(attributes);

        sysUtilMock.when(() -> SysUtil.getIpAddress(any())).thenReturn("127.0.0.1");

        // Call private method via reflection
        try {
            var method = AuthenticationEventListener.class.getDeclaredMethod("handleLoginSuccess", LoginSuccessfulEvent.class);
            method.setAccessible(true);
            method.invoke(listener, new LoginSuccessfulEvent(authentication));
        } catch (Exception e) {
            fail("Failed to invoke handleLoginSuccess", e);
        }

        verify(loginLogService).createLoginLog(any(LoginLog.class));
    }

    @Test
    void handleLoginFailure_CreatesLoginLog() {
        AuthenticationResponse response = AuthenticationResponse.failure("Invalid credentials");
        LoginFailedEvent event = new LoginFailedEvent(response);

        HttpRequest<Object> request = mock(HttpRequest.class);
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("testuser");
        loginReq.setTenantId("tenant1");

        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.of(request));
        when(request.getBody(LoginRequest.class)).thenReturn(Optional.of(loginReq));
        sysUtilMock.when(() -> SysUtil.getIpAddress(any())).thenReturn("127.0.0.1");

        // Call private method via reflection
        try {
            var method = AuthenticationEventListener.class.getDeclaredMethod("handleLoginFailure", LoginFailedEvent.class);
            method.setAccessible(true);
            method.invoke(listener, event);
        } catch (Exception e) {
            fail("Failed to invoke handleLoginFailure", e);
        }

        verify(loginLogService).createLoginLog(any(LoginLog.class));
    }

    @Test
    void handleLoginFailure_NoMessage_UsesDefault() {
        AuthenticationResponse response = mock(AuthenticationResponse.class);
        when(response.getMessage()).thenReturn(Optional.empty());
        LoginFailedEvent event = new LoginFailedEvent(response);

        HttpRequest<Object> request = mock(HttpRequest.class);
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("testuser");
        loginReq.setTenantId("tenant1");

        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.of(request));
        when(request.getBody(LoginRequest.class)).thenReturn(Optional.of(loginReq));
        sysUtilMock.when(() -> SysUtil.getIpAddress(any())).thenReturn("127.0.0.1");

        // Call private method via reflection
        try {
            var method = AuthenticationEventListener.class.getDeclaredMethod("handleLoginFailure", LoginFailedEvent.class);
            method.setAccessible(true);
            method.invoke(listener, event);
        } catch (Exception e) {
            fail("Failed to invoke handleLoginFailure", e);
        }

        verify(loginLogService).createLoginLog(any(LoginLog.class));
    }

    @Test
    void handleLoginFailure_NoRequestBody_UsesDefaults() {
        AuthenticationResponse response = AuthenticationResponse.failure("Invalid credentials");
        LoginFailedEvent event = new LoginFailedEvent(response);

        HttpRequest<Object> request = mock(HttpRequest.class);

        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.of(request));
        when(request.getBody(LoginRequest.class)).thenReturn(Optional.empty());
        sysUtilMock.when(() -> SysUtil.getIpAddress(any())).thenReturn("127.0.0.1");

        // Call private method via reflection
        try {
            var method = AuthenticationEventListener.class.getDeclaredMethod("handleLoginFailure", LoginFailedEvent.class);
            method.setAccessible(true);
            method.invoke(listener, event);
        } catch (Exception e) {
            fail("Failed to invoke handleLoginFailure", e);
        }

        verify(loginLogService).createLoginLog(any(LoginLog.class));
    }

    @Test
    void handleLoginFailure_NoRequest_UsesDefaults() {
        AuthenticationResponse response = AuthenticationResponse.failure("Invalid credentials");
        LoginFailedEvent event = new LoginFailedEvent(response);

        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.empty());
        sysUtilMock.when(() -> SysUtil.getIpAddress(any())).thenReturn("127.0.0.1");

        // Call private method via reflection
        try {
            var method = AuthenticationEventListener.class.getDeclaredMethod("handleLoginFailure", LoginFailedEvent.class);
            method.setAccessible(true);
            method.invoke(listener, event);
        } catch (Exception e) {
            fail("Failed to invoke handleLoginFailure", e);
        }

        verify(loginLogService).createLoginLog(any(LoginLog.class));
    }

    @Test
    void handleLoginFailure_SourceNotAuthenticationResponse() {
        Object otherSource = "some other source";
        LoginFailedEvent event = new LoginFailedEvent(otherSource);

        HttpRequest<Object> request = mock(HttpRequest.class);
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("testuser");
        loginReq.setTenantId("tenant1");

        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.of(request));
        when(request.getBody(LoginRequest.class)).thenReturn(Optional.of(loginReq));
        sysUtilMock.when(() -> SysUtil.getIpAddress(any())).thenReturn("127.0.0.1");

        // Call private method via reflection
        try {
            var method = AuthenticationEventListener.class.getDeclaredMethod("handleLoginFailure", LoginFailedEvent.class);
            method.setAccessible(true);
            method.invoke(listener, event);
        } catch (Exception e) {
            fail("Failed to invoke handleLoginFailure", e);
        }

        verify(loginLogService).createLoginLog(any(LoginLog.class));
    }

    @Test
    void getIpAddress_WithRequest_ReturnsIp() {
        HttpRequest<Object> request = mock(HttpRequest.class);
        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.of(request));
        sysUtilMock.when(() -> SysUtil.getIpAddress(request)).thenReturn("127.0.0.1");

        // Call private method via reflection
        try {
            var method = AuthenticationEventListener.class.getDeclaredMethod("getIpAddress");
            method.setAccessible(true);
            String result = (String) method.invoke(listener);
            assertEquals("127.0.0.1", result);
        } catch (Exception e) {
            fail("Failed to invoke getIpAddress", e);
        }
    }

    @Test
    void getIpAddress_NoRequest_ReturnsUnknown() {
        serverRequestContextMock.when(ServerRequestContext::currentRequest).thenReturn(Optional.empty());

        // Call private method via reflection
        try {
            var method = AuthenticationEventListener.class.getDeclaredMethod("getIpAddress");
            method.setAccessible(true);
            String result = (String) method.invoke(listener);
            assertEquals("Unknown", result);
        } catch (Exception e) {
            fail("Failed to invoke getIpAddress", e);
        }
    }
}