package com.mok.sys.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mok.sys.application.dto.auth.LoginRequest;
import com.mok.sys.application.dto.auth.LoginResDTO;
import com.mok.sys.infrastructure.security.CustomUserDetail;
import com.mok.sys.infrastructure.security.JwtTokenProvider;
import com.mok.common.web.RestResponse;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.Authenticator;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private Authenticator<HttpRequest<?>> authenticator;
    private JwtTokenProvider tokenProvider;
    private AuthController authController;
    private HttpRequest<?> request;

    @BeforeEach
    void setUp() {
        authenticator = mock(Authenticator.class);
        tokenProvider = mock(JwtTokenProvider.class);
        authController = new AuthController(authenticator, tokenProvider);
        request = mock(HttpRequest.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
    }

    @Test
    void loginSuccess() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password");
        loginRequest.setTenantId("tenant1");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin");
        when(authentication.getAttributes()).thenReturn(Map.of(
                "userId", 1L,
                "roleIds", Collections.singletonList(1L),
                "isSuperAdmin", true
        ));
        when(authentication.getRoles()).thenReturn(Collections.singletonList("ADMIN"));

        String authName = authentication.getName();
        var authRoles = authentication.getRoles();
        var authAttributes = authentication.getAttributes();

        when(authenticator.authenticate(any(), any(UsernamePasswordCredentials.class)))
                .thenReturn(Mono.just(AuthenticationResponse.success(authName, authRoles, authAttributes)));

        when(tokenProvider.createToken(anyString(), anyString(), any(CustomUserDetail.class), any(), any()))
                .thenReturn("mock-jwt-token");

        Mono<RestResponse<LoginResDTO>> responseMono = authController.authenticateUser(loginRequest, request);
        RestResponse<LoginResDTO> response = responseMono.block();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertNotNull(response.getData());
        assertEquals("mock-jwt-token", response.getData().getToken());
        assertEquals("admin", response.getData().getUsername());
        assertEquals("tenant1", response.getData().getTenantId());

        verify(tokenProvider).createToken(eq("admin"), eq("tenant1"), any(CustomUserDetail.class), any(), any());
    }

    @Test
    void loginFailure_AuthenticationFailed() throws JsonProcessingException {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("wrong-password");
        loginRequest.setTenantId("tenant1");

        when(authenticator.authenticate(any(), any(UsernamePasswordCredentials.class)))
                .thenReturn(Mono.just(AuthenticationResponse.failure("Invalid credentials")));

        Mono<RestResponse<LoginResDTO>> responseMono = authController.authenticateUser(loginRequest, request);
        RestResponse<LoginResDTO> response = responseMono.block();

        assertNotNull(response);
        assertEquals(400, response.getCode());
        assertFalse(response.isState());
        assertEquals("Invalid credentials", response.getMessage());
        assertNull(response.getData());

        verify(tokenProvider, never()).createToken(anyString(), anyString(), any(), anyString(), anyString()); // Should not be called
    }

    @Test
    void loginFailure_AuthenticatedButNoAuthentication() throws JsonProcessingException {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password");
        loginRequest.setTenantId("tenant1");

        // Create a custom response that is authenticated but has empty authentication
        AuthenticationResponse customResponse = new AuthenticationResponse() {
            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public Optional<Authentication> getAuthentication() {
                return Optional.empty();
            }

            @Override
            public Optional<String> getMessage() {
                return Optional.of("Authenticated but no auth object");
            }
        };

        when(authenticator.authenticate(any(), any(UsernamePasswordCredentials.class)))
                .thenReturn(Mono.just(customResponse));

        Mono<RestResponse<LoginResDTO>> responseMono = authController.authenticateUser(loginRequest, request);
        RestResponse<LoginResDTO> response = responseMono.block();

        assertNotNull(response);
        assertEquals(400, response.getCode());
        assertFalse(response.isState());
        assertEquals("Authenticated but no auth object", response.getMessage());
        assertNull(response.getData());

        verify(tokenProvider, never()).createToken(anyString(), anyString(), any(), anyString(), anyString()); // Should not be called
    }

    @Test
    void loginFailure_TokenCreationError() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password");
        loginRequest.setTenantId("tenant1");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin");
        when(authentication.getAttributes()).thenReturn(Map.of("userId", 1L));

        when(authenticator.authenticate(any(), any(UsernamePasswordCredentials.class)))
                .thenReturn(Mono.just(AuthenticationResponse.success("admin")));

        when(tokenProvider.createToken(anyString(), anyString(), any(CustomUserDetail.class), any(), any()))
                .thenThrow(new RuntimeException("Redis error"));

        Mono<RestResponse<LoginResDTO>> responseMono = authController.authenticateUser(loginRequest, request);
        RestResponse<LoginResDTO> response = responseMono.block();

        assertNotNull(response);
        assertEquals(400, response.getCode());
        assertFalse(response.isState());
        assertEquals("令牌创建失败", response.getMessage());
    }

    @Test
    void logoutSuccess() {
        HttpHeaders headers = mock(HttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getAuthorization()).thenReturn(Optional.of("Bearer mock-token"));

        RestResponse<Void> response = authController.logout(request);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(tokenProvider).removeToken("mock-token");
    }

    @Test
    void logout_NoToken() {
        HttpHeaders headers = mock(HttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getAuthorization()).thenReturn(Optional.empty());

        RestResponse<Void> response = authController.logout(request);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(tokenProvider, never()).removeToken(anyString());
    }

    @Test
    void logout_InvalidTokenFormat() {
        HttpHeaders headers = mock(HttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getAuthorization()).thenReturn(Optional.of("Basic auth"));

        RestResponse<Void> response = authController.logout(request);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(tokenProvider, never()).removeToken(anyString());
    }

    @Test
    void loginSuccess_WithStringRoleIds() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password");
        loginRequest.setTenantId("tenant1");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin");
        when(authentication.getAttributes()).thenReturn(Map.of(
                "userId", 1L,
                "roleIds", "invalid", // Not a Collection
                "isSuperAdmin", false
        ));
        when(authentication.getRoles()).thenReturn(Collections.singletonList("USER"));

        String authName = authentication.getName();
        var authRoles = authentication.getRoles();
        var authAttributes = authentication.getAttributes();

        when(authenticator.authenticate(any(), any(UsernamePasswordCredentials.class)))
                .thenReturn(Mono.just(AuthenticationResponse.success(authName, authRoles, authAttributes)));

        when(tokenProvider.createToken(anyString(), anyString(), any(CustomUserDetail.class), any(), any()))
                .thenReturn("mock-jwt-token");

        Mono<RestResponse<LoginResDTO>> responseMono = authController.authenticateUser(loginRequest, request);
        RestResponse<LoginResDTO> response = responseMono.block();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertNotNull(response.getData());
        assertEquals("mock-jwt-token", response.getData().getToken());

        verify(tokenProvider).createToken(eq("admin"), eq("tenant1"), any(CustomUserDetail.class), any(), any());
    }

    @Test
    void loginSuccess_WithNullUserId() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password");
        loginRequest.setTenantId("tenant1");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", null); // Null userId
        attributes.put("roleIds", Collections.singletonList(1L));
        attributes.put("isSuperAdmin", true);
        when(authentication.getAttributes()).thenReturn(attributes);
        when(authentication.getRoles()).thenReturn(Collections.singletonList("ADMIN"));

        String authName = authentication.getName();
        var authRoles = authentication.getRoles();
        var authAttributes = authentication.getAttributes();

        when(authenticator.authenticate(any(), any(UsernamePasswordCredentials.class)))
                .thenReturn(Mono.just(AuthenticationResponse.success(authName, authRoles, authAttributes)));

        when(tokenProvider.createToken(anyString(), anyString(), any(CustomUserDetail.class), any(), any()))
                .thenReturn("mock-jwt-token");

        Mono<RestResponse<LoginResDTO>> responseMono = authController.authenticateUser(loginRequest, request);
        RestResponse<LoginResDTO> response = responseMono.block();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertNotNull(response.getData());
        assertEquals("mock-jwt-token", response.getData().getToken());

        verify(tokenProvider).createToken(eq("admin"), eq("tenant1"), any(CustomUserDetail.class), any(), any());
    }

    @Test
    void loginSuccess_WithNullRoleIds() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password");
        loginRequest.setTenantId("tenant1");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", 1L);
        attributes.put("roleIds", null); // Null roleIds
        attributes.put("isSuperAdmin", false);
        when(authentication.getAttributes()).thenReturn(attributes);
        when(authentication.getRoles()).thenReturn(Collections.singletonList("USER"));

        String authName = authentication.getName();
        var authRoles = authentication.getRoles();
        var authAttributes = authentication.getAttributes();

        when(authenticator.authenticate(any(), any(UsernamePasswordCredentials.class)))
                .thenReturn(Mono.just(AuthenticationResponse.success(authName, authRoles, authAttributes)));

        when(tokenProvider.createToken(anyString(), anyString(), any(CustomUserDetail.class), any(), any()))
                .thenReturn("mock-jwt-token");

        Mono<RestResponse<LoginResDTO>> responseMono = authController.authenticateUser(loginRequest, request);
        RestResponse<LoginResDTO> response = responseMono.block();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertNotNull(response.getData());
        assertEquals("mock-jwt-token", response.getData().getToken());

        verify(tokenProvider).createToken(eq("admin"), eq("tenant1"), any(CustomUserDetail.class), any(), any());
    }
}
