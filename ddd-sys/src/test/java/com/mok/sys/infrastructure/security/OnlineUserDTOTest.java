package com.mok.sys.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OnlineUserDTOTest {

    @Test
    void constructor_SetsFields() {
        Long userId = 1L;
        String username = "testuser";
        String tenantId = "tenant1";
        String tenantName = "Tenant One";
        List<OnlineUserDTO.SessionDetail> sessions = List.of(
                new OnlineUserDTO.SessionDetail("session1", "127.0.0.1", "Chrome", 1234567890L)
        );

        OnlineUserDTO dto = new OnlineUserDTO(userId, username, tenantId, tenantName, sessions);

        assertEquals(userId, dto.userId());
        assertEquals(username, dto.username());
        assertEquals(tenantId, dto.tenantId());
        assertEquals(tenantName, dto.tenantName());
        assertEquals(sessions, dto.sessions());
    }

    @Test
    void sessionDetailConstructor_SetsFields() {
        String id = "session1";
        String ip = "127.0.0.1";
        String browser = "Chrome";
        long loginTime = 1234567890L;

        OnlineUserDTO.SessionDetail detail = new OnlineUserDTO.SessionDetail(id, ip, browser, loginTime);

        assertEquals(id, detail.id());
        assertEquals(ip, detail.ip());
        assertEquals(browser, detail.browser());
        assertEquals(loginTime, detail.loginTime());
    }

    @Test
    void equalsAndHashCode_WorkCorrectly() {
        OnlineUserDTO dto1 = new OnlineUserDTO(1L, "user", "tenant1", "Tenant", List.of());
        OnlineUserDTO dto2 = new OnlineUserDTO(1L, "user", "tenant1", "Tenant", List.of());
        OnlineUserDTO dto3 = new OnlineUserDTO(2L, "user", "tenant1", "Tenant", List.of());

        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertNotEquals(dto1.hashCode(), dto3.hashCode());
    }

    @Test
    void toString_Works() {
        OnlineUserDTO dto = new OnlineUserDTO(1L, "user", "tenant1", "Tenant", List.of());

        String toString = dto.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("userId=1"));
        assertTrue(toString.contains("username=user"));
    }
}