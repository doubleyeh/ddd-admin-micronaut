package com.mok.application.sys.listener;

import com.mok.application.sys.dto.user.UserPostDTO;
import com.mok.application.sys.event.TenantCreatedEvent;
import com.mok.application.sys.service.UserService;
import com.mok.domain.sys.model.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserCreationListenerTest {

    private UserService userService;
    private UserCreationListener listener;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        listener = new UserCreationListener(userService);
    }

    @Test
    void onTenantCreated_CreatesDefaultAdminUser() {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getTenantId()).thenReturn("tenant123");
        when(tenant.getName()).thenReturn("Test Tenant");

        String rawPassword = "password123";

        TenantCreatedEvent event = new TenantCreatedEvent(tenant, rawPassword);

        listener.onTenantCreated(event);

        ArgumentCaptor<UserPostDTO> captor = ArgumentCaptor.forClass(UserPostDTO.class);
        verify(userService).createForTenant(captor.capture());

        UserPostDTO captured = captor.getValue();
        assertEquals("admin", captured.getUsername());
        assertEquals("Test Tenant管理员", captured.getNickname());
        assertEquals(rawPassword, captured.getPassword());
        assertEquals(1, captured.getState());
        assertEquals("tenant123", captured.getTenantId());
    }
}