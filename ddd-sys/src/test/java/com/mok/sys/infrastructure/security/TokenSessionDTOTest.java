package com.mok.sys.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenSessionDTOTest {

    @Test
    void constructor_SetsFields() {
        String username = "testuser";
        String tenantId = "tenant1";
        CustomUserDetail principal = null; // Can be null
        String ip = "127.0.0.1";
        String browser = "Chrome";
        long loginTime = 1234567890L;

        TokenSessionDTO dto = new TokenSessionDTO(username, tenantId, principal, ip, browser, loginTime);

        assertEquals(username, dto.getUsername());
        assertEquals(tenantId, dto.getTenantId());
        assertEquals(principal, dto.getPrincipal());
        assertEquals(ip, dto.getIp());
        assertEquals(browser, dto.getBrowser());
        assertEquals(loginTime, dto.getLoginTime());
    }

    @Test
    void defaultConstructor_CreatesEmptyObject() {
        TokenSessionDTO dto = new TokenSessionDTO();

        assertNull(dto.getToken());
        assertNull(dto.getUsername());
        assertNull(dto.getTenantId());
        assertNull(dto.getPrincipal());
        assertNull(dto.getIp());
        assertNull(dto.getBrowser());
        assertEquals(0L, dto.getLoginTime());
    }

    @Test
    void settersAndGetters_WorkCorrectly() {
        TokenSessionDTO dto = new TokenSessionDTO();

        String token = "testToken";
        String username = "testuser";
        String tenantId = "tenant1";
        CustomUserDetail principal = null;
        String ip = "127.0.0.1";
        String browser = "Chrome";
        long loginTime = 1234567890L;

        dto.setToken(token);
        dto.setUsername(username);
        dto.setTenantId(tenantId);
        dto.setPrincipal(principal);
        dto.setIp(ip);
        dto.setBrowser(browser);
        dto.setLoginTime(loginTime);

        assertEquals(token, dto.getToken());
        assertEquals(username, dto.getUsername());
        assertEquals(tenantId, dto.getTenantId());
        assertEquals(principal, dto.getPrincipal());
        assertEquals(ip, dto.getIp());
        assertEquals(browser, dto.getBrowser());
        assertEquals(loginTime, dto.getLoginTime());
    }
}