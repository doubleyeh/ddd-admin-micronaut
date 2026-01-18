package com.mok.infrastructure.tenant;

import com.mok.infrastructure.util.SysUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class TenantContextHolderTest {

    private MockedStatic<SysUtil> sysUtilMock;

    @BeforeEach
    void setUp() {
        sysUtilMock = mockStatic(SysUtil.class);
    }

    @AfterEach
    void tearDown() {
        sysUtilMock.close();
    }

    @Test
    void getTenantId_WhenNotBound_ReturnsEmptyString() {
        String tenantId = TenantContextHolder.getTenantId();
        assertEquals("", tenantId);
    }

    @Test
    void getUsername_WhenNotBound_ReturnsEmptyString() {
        String username = TenantContextHolder.getUsername();
        assertEquals("", username);
    }

    @Test
    void getUserId_WhenNotBound_ReturnsNull() {
        Long userId = TenantContextHolder.getUserId();
        assertNull(userId);
    }

    @Test
    void getTenantId_WhenBound_ReturnsValue() {
        ScopedValue.where(TenantContextHolder.TENANT_ID, "tenant1").run(() -> {
            String tenantId = TenantContextHolder.getTenantId();
            assertEquals("tenant1", tenantId);
        });
    }

    @Test
    void getUsername_WhenBound_ReturnsValue() {
        ScopedValue.where(TenantContextHolder.USERNAME, "user1").run(() -> {
            String username = TenantContextHolder.getUsername();
            assertEquals("user1", username);
        });
    }

    @Test
    void getUserId_WhenBound_ReturnsValue() {
        ScopedValue.where(TenantContextHolder.USER_ID, 123L).run(() -> {
            Long userId = TenantContextHolder.getUserId();
            assertEquals(123L, userId);
        });
    }

    @Test
    void isSuperAdmin_ReturnsTrue() {
        ScopedValue.where(TenantContextHolder.TENANT_ID, "super").where(TenantContextHolder.USERNAME, "admin").run(() -> {
            sysUtilMock.when(() -> SysUtil.isSuperAdmin("super", "admin")).thenReturn(true);
            boolean result = TenantContextHolder.isSuperAdmin();
            assertTrue(result);
        });
    }

    @Test
    void isSuperAdmin_ReturnsFalse() {
        ScopedValue.where(TenantContextHolder.TENANT_ID, "tenant1").where(TenantContextHolder.USERNAME, "user").run(() -> {
            sysUtilMock.when(() -> SysUtil.isSuperAdmin("tenant1", "user")).thenReturn(false);
            boolean result = TenantContextHolder.isSuperAdmin();
            assertFalse(result);
        });
    }

    @Test
    void isSuperTenant_ReturnsTrue() {
        ScopedValue.where(TenantContextHolder.TENANT_ID, "super").run(() -> {
            sysUtilMock.when(() -> SysUtil.isSuperTenant("super")).thenReturn(true);
            boolean result = TenantContextHolder.isSuperTenant();
            assertTrue(result);
        });
    }

    @Test
    void isSuperTenant_ReturnsFalse() {
        ScopedValue.where(TenantContextHolder.TENANT_ID, "tenant1").run(() -> {
            sysUtilMock.when(() -> SysUtil.isSuperTenant("tenant1")).thenReturn(false);
            boolean result = TenantContextHolder.isSuperTenant();
            assertFalse(result);
        });
    }
}
