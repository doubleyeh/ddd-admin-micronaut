package com.mok.web.sys;

import com.mok.application.sys.dto.tenant.TenantOptionDTO;
import com.mok.application.sys.service.TenantService;
import com.mok.infrastructure.security.JwtTokenProvider;
import com.mok.infrastructure.security.OnlineUserDTO;
import com.mok.infrastructure.tenant.TenantContextHolder;
import com.mok.web.common.RestResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnlineUserControllerTest {

    private JwtTokenProvider tokenProvider;
    private TenantService tenantService;
    private OnlineUserController onlineUserController;
    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    @BeforeEach
    void setUp() {
        tokenProvider = mock(JwtTokenProvider.class);
        tenantService = mock(TenantService.class);
        onlineUserController = new OnlineUserController(tokenProvider, tenantService);
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
    }

    @Test
    void list() {
        String tenantId = "tenant1";
        TenantOptionDTO tenantOption = new TenantOptionDTO(1L, tenantId, "Tenant One");
        List<TenantOptionDTO> tenantOptions = Collections.singletonList(tenantOption);
        List<OnlineUserDTO> onlineUsers = Collections.singletonList(new OnlineUserDTO(1L, "user", tenantId, "Tenant One", Collections.emptyList()));

        when(tenantService.findOptions(null)).thenReturn(tenantOptions);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(tenantId);
        tenantContextHolderMock.when(TenantContextHolder::isSuperTenant).thenReturn(false);
        when(tokenProvider.getAllOnlineUsers(anyMap(), eq(tenantId), eq(false))).thenReturn(onlineUsers);

        RestResponse<List<OnlineUserDTO>> response = onlineUserController.list();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(onlineUsers, response.getData());

        verify(tenantService).findOptions(null);
        verify(tokenProvider).getAllOnlineUsers(anyMap(), eq(tenantId), eq(false));
    }

    @Test
    void list_SuperTenant() {
        String tenantId = "super";
        TenantOptionDTO tenantOption = new TenantOptionDTO(1L, "tenant1", "Tenant One");
        List<TenantOptionDTO> tenantOptions = Collections.singletonList(tenantOption);
        List<OnlineUserDTO> onlineUsers = Collections.singletonList(new OnlineUserDTO(1L, "user", "tenant1", "Tenant One", Collections.emptyList()));

        when(tenantService.findOptions(null)).thenReturn(tenantOptions);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(tenantId);
        tenantContextHolderMock.when(TenantContextHolder::isSuperTenant).thenReturn(true);
        when(tokenProvider.getAllOnlineUsers(anyMap(), eq(tenantId), eq(true))).thenReturn(onlineUsers);

        RestResponse<List<OnlineUserDTO>> response = onlineUserController.list();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(onlineUsers, response.getData());

        verify(tenantService).findOptions(null);
        verify(tokenProvider).getAllOnlineUsers(anyMap(), eq(tenantId), eq(true));
    }

    @Test
    void list_WithDuplicateTenantIds() {
        String tenantId = "tenant1";
        TenantOptionDTO tenantOption1 = new TenantOptionDTO(1L, tenantId, "Tenant One");
        TenantOptionDTO tenantOption2 = new TenantOptionDTO(2L, tenantId, "Tenant Duplicate"); // Same tenantId
        List<TenantOptionDTO> tenantOptions = List.of(tenantOption1, tenantOption2);
        List<OnlineUserDTO> onlineUsers = Collections.singletonList(new OnlineUserDTO(1L, "user", tenantId, "Tenant One", Collections.emptyList()));

        when(tenantService.findOptions(null)).thenReturn(tenantOptions);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(tenantId);
        tenantContextHolderMock.when(TenantContextHolder::isSuperTenant).thenReturn(false);
        when(tokenProvider.getAllOnlineUsers(anyMap(), eq(tenantId), eq(false))).thenReturn(onlineUsers);

        RestResponse<List<OnlineUserDTO>> response = onlineUserController.list();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(onlineUsers, response.getData());

        verify(tenantService).findOptions(null);
        verify(tokenProvider).getAllOnlineUsers(anyMap(), eq(tenantId), eq(false));
    }

    @Test
    void kickout() {
        String token = "test-token";
        Map<String, String> body = Collections.singletonMap("token", token);
        doNothing().when(tokenProvider).removeToken(token);

        RestResponse<Void> response = onlineUserController.kickout(body);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(tokenProvider).removeToken(token);
    }

    @Test
    void kickout_noToken() {
        Map<String, String> body = Collections.emptyMap();

        RestResponse<Void> response = onlineUserController.kickout(body);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(tokenProvider, never()).removeToken(any());
    }
}