package com.mok.sys.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ScanIterator;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtTokenProviderTest {

    private RedisCommands<String, String> redisCommands;
    private ObjectMapper objectMapper;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        redisCommands = mock(RedisCommands.class);
        objectMapper = new ObjectMapper();
        jwtTokenProvider = new JwtTokenProvider(redisCommands, objectMapper);
    }

    @Test
    void createToken_AllowMultiDevice_Success() throws Exception {
        try {
            var allowMultiDeviceField = JwtTokenProvider.class.getDeclaredField("allowMultiDevice");
            allowMultiDeviceField.setAccessible(true);
            allowMultiDeviceField.set(jwtTokenProvider, true);
        } catch (Exception e) {
            fail("Failed to set private field", e);
        }

        String username = "testuser";
        String tenantId = "tenant1";
        com.mok.common.infrastructure.security.CustomUserDetail principal = mock(com.mok.common.infrastructure.security.CustomUserDetail.class);
        String ip = "127.0.0.1";
        String browser = "Chrome";

        String token = jwtTokenProvider.createToken(username, tenantId, principal, ip, browser);

        assertNotNull(token);
        verify(redisCommands).setex(anyString(), anyLong(), anyString());
        verify(redisCommands).sadd(anyString(), eq(token));
        verify(redisCommands).expire(anyString(), anyLong());
    }

    @Test
    void createToken_NotAllowMultiDevice_Success() throws Exception {
        // Set allowMultiDevice to false
        try {
            var allowMultiDeviceField = JwtTokenProvider.class.getDeclaredField("allowMultiDevice");
            allowMultiDeviceField.setAccessible(true);
            allowMultiDeviceField.set(jwtTokenProvider, false);
        } catch (Exception e) {
            fail("Failed to set private field", e);
        }

        String username = "testuser";
        String tenantId = "tenant1";
        com.mok.common.infrastructure.security.CustomUserDetail principal = mock(com.mok.common.infrastructure.security.CustomUserDetail.class);
        String ip = "127.0.0.1";
        String browser = "Chrome";

        when(redisCommands.get(anyString())).thenReturn("oldToken");

        String token = jwtTokenProvider.createToken(username, tenantId, principal, ip, browser);

        assertNotNull(token);
        verify(redisCommands).get(anyString());
        verify(redisCommands).del(anyString());
        verify(redisCommands).setex(anyString(), anyLong(), anyString());
        verify(redisCommands).set(anyString(), eq(token));
        verify(redisCommands).expire(anyString(), anyLong());
    }

    @Test
    void getSession_ValidToken_ReturnsSession() throws Exception {
        String token = "testToken";
        String sessionJson = "{\"token\":\"testToken\",\"username\":\"user\",\"tenantId\":\"tenant1\",\"ip\":\"127.0.0.1\",\"browser\":\"Chrome\",\"loginTime\":1234567890}";
        com.mok.common.infrastructure.security.TokenSessionDTO expectedSession = new com.mok.common.infrastructure.security.TokenSessionDTO("user", "tenant1", null, "127.0.0.1", "Chrome", 1234567890);

        when(redisCommands.get(anyString())).thenReturn(sessionJson);
        when(redisCommands.ttl(anyString())).thenReturn(700L); // > 600, no refresh

        com.mok.common.infrastructure.security.TokenSessionDTO session = jwtTokenProvider.getSession(token);

        assertNotNull(session);
        assertEquals("user", session.getUsername());
        assertEquals("tenant1", session.getTenantId());
        verify(redisCommands, never()).expire(anyString(), anyLong());
    }

    @Test
    void getSession_TokenNeedsRefresh_Refreshes() throws Exception {
        String token = "testToken";
        String sessionJson = "{\"token\":\"testToken\",\"username\":\"user\",\"tenantId\":\"tenant1\",\"ip\":\"127.0.0.1\",\"browser\":\"Chrome\",\"loginTime\":1234567890}";
        com.mok.common.infrastructure.security.TokenSessionDTO expectedSession = new com.mok.common.infrastructure.security.TokenSessionDTO("user", "tenant1", null, "127.0.0.1", "Chrome", 1234567890);

        when(redisCommands.get(anyString())).thenReturn(sessionJson);
        when(redisCommands.ttl(anyString())).thenReturn(300L); // < 600, refresh

        com.mok.common.infrastructure.security.TokenSessionDTO session = jwtTokenProvider.getSession(token);

        assertNotNull(session);
        assertEquals("user", session.getUsername());
        verify(redisCommands, times(2)).expire(anyString(), anyLong());
    }

    @Test
    void getSession_TokenExpired_NoRefresh() throws Exception {
        String token = "testToken";
        String sessionJson = "{\"token\":\"testToken\",\"username\":\"user\",\"tenantId\":\"tenant1\",\"ip\":\"127.0.0.1\",\"browser\":\"Chrome\",\"loginTime\":1234567890}";
        com.mok.common.infrastructure.security.TokenSessionDTO expectedSession = new com.mok.common.infrastructure.security.TokenSessionDTO("user", "tenant1", null, "127.0.0.1", "Chrome", 1234567890);

        when(redisCommands.get(anyString())).thenReturn(sessionJson);
        when(redisCommands.ttl(anyString())).thenReturn(-1L); // Expired

        com.mok.common.infrastructure.security.TokenSessionDTO session = jwtTokenProvider.getSession(token);

        assertNotNull(session);
        assertEquals("user", session.getUsername());
        verify(redisCommands, never()).expire(anyString(), anyLong());
    }

    @Test
    void getSession_TokenNotExpired_NoRefresh() throws Exception {
        String token = "testToken";
        String sessionJson = "{\"token\":\"testToken\",\"username\":\"user\",\"tenantId\":\"tenant1\",\"ip\":\"127.0.0.1\",\"browser\":\"Chrome\",\"loginTime\":1234567890}";
        com.mok.common.infrastructure.security.TokenSessionDTO expectedSession = new com.mok.common.infrastructure.security.TokenSessionDTO("user", "tenant1", null, "127.0.0.1", "Chrome", 1234567890);

        when(redisCommands.get(anyString())).thenReturn(sessionJson);
        when(redisCommands.ttl(anyString())).thenReturn(700L); // > 600, no refresh

        com.mok.common.infrastructure.security.TokenSessionDTO session = jwtTokenProvider.getSession(token);

        assertNotNull(session);
        assertEquals("user", session.getUsername());
        verify(redisCommands, never()).expire(anyString(), anyLong());
    }

    @Test
    void getSession_InvalidJson_ReturnsNull() {
        String token = "testToken";
        String invalidJson = "invalid json";

        when(redisCommands.get(anyString())).thenReturn(invalidJson);

        com.mok.common.infrastructure.security.TokenSessionDTO session = jwtTokenProvider.getSession(token);

        assertNull(session);
    }

    @Test
    void getSession_NoData_ReturnsNull() {
        String token = "testToken";

        when(redisCommands.get(anyString())).thenReturn(null);

        com.mok.common.infrastructure.security.TokenSessionDTO session = jwtTokenProvider.getSession(token);

        assertNull(session);
    }

    @Test
    void getAllOnlineUsers_SuperUser_ReturnsAll() {
        Map<String, String> tenantMap = Map.of("tenant1", "Tenant One");
        String currentTenantId = "tenant1";
        boolean isSuper = true;

        ScanIterator<String> scanIterator = mock(ScanIterator.class);
        when(scanIterator.hasNext()).thenReturn(true, false);
        when(scanIterator.next()).thenReturn("auth:token:token1");

        try (var mockedStatic = mockStatic(ScanIterator.class)) {
            mockedStatic.when(() -> ScanIterator.scan(any(), any(ScanArgs.class))).thenReturn(scanIterator);

            when(redisCommands.get(anyString())).thenReturn("{\"username\":\"user1\",\"tenantId\":\"tenant1\"}");

            List<OnlineUserDTO> result = jwtTokenProvider.getAllOnlineUsers(tenantMap, currentTenantId, isSuper);

            assertNotNull(result);
        }
    }

    @Test
    void getAllOnlineUsers_NonSuperUser_ReturnsFiltered() {
        Map<String, String> tenantMap = Map.of("tenant1", "Tenant One", "tenant2", "Tenant Two");
        String currentTenantId = "tenant1";
        boolean isSuper = false;

        ScanIterator<String> scanIterator = mock(ScanIterator.class);
        when(scanIterator.hasNext()).thenReturn(true, true, true, false);
        when(scanIterator.next()).thenReturn("auth:token:token1", "auth:token:token2", "auth:token:token3");

        try (var mockedStatic = mockStatic(ScanIterator.class)) {
            mockedStatic.when(() -> ScanIterator.scan(any(), any(ScanArgs.class))).thenReturn(scanIterator);

            when(redisCommands.get("auth:token:token1")).thenReturn("{\"username\":\"user1\",\"tenantId\":\"tenant1\",\"principal\":{\"userId\":1,\"username\":\"user1\",\"tenantId\":\"tenant1\"}}");
            when(redisCommands.get("auth:token:token2")).thenReturn("{\"username\":\"user2\",\"tenantId\":\"tenant2\",\"principal\":{\"userId\":2,\"username\":\"user2\",\"tenantId\":\"tenant2\"}}");
            when(redisCommands.get("auth:token:token3")).thenReturn("{\"username\":\"user3\",\"tenantId\":\"tenant1\",\"principal\":null}");

            List<OnlineUserDTO> result = jwtTokenProvider.getAllOnlineUsers(tenantMap, currentTenantId, isSuper);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("user1", result.getFirst().username());
            assertEquals("tenant1", result.getFirst().tenantId());
        }
    }

    @Test
    void removeToken_AllowMultiDevice_RemovesFromSet() {
        // Set allowMultiDevice to true
        try {
            var allowMultiDeviceField = JwtTokenProvider.class.getDeclaredField("allowMultiDevice");
            allowMultiDeviceField.setAccessible(true);
            allowMultiDeviceField.set(jwtTokenProvider, true);
        } catch (Exception e) {
            fail("Failed to set private field", e);
        }

        String token = "testToken";
        com.mok.common.infrastructure.security.TokenSessionDTO session = new com.mok.common.infrastructure.security.TokenSessionDTO("user", "tenant1", null, "127.0.0.1", "Chrome", 1234567890);

        when(redisCommands.get(anyString())).thenReturn("{\"username\":\"user\",\"tenantId\":\"tenant1\"}");

        jwtTokenProvider.removeToken(token);

        verify(redisCommands).del(anyString());
        verify(redisCommands).srem(anyString(), eq(token));
    }

    @Test
    void removeToken_NotAllowMultiDevice_RemovesKey() {
        String token = "testToken";
        com.mok.common.infrastructure.security.TokenSessionDTO session = new com.mok.common.infrastructure.security.TokenSessionDTO("user", "tenant1", null, "127.0.0.1", "Chrome", 1234567890);

        when(redisCommands.get(anyString())).thenReturn("{\"username\":\"user\",\"tenantId\":\"tenant1\"}");

        jwtTokenProvider.removeToken(token);

        verify(redisCommands, times(2)).del(anyString());
    }

    @Test
    void removeToken_NoSession_DoesNothing() {
        String token = "testToken";

        when(redisCommands.get(anyString())).thenReturn(null);

        jwtTokenProvider.removeToken(token);

        verify(redisCommands, never()).del(anyString());
    }
}
