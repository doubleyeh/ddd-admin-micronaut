package com.mok.common.infrastructure.security;

public interface TokenProvider {
    String createToken(String username, String tenantId, CustomUserDetail principal, String ipAddress, String browser) throws Exception;

    TokenSessionDTO getSession(String token);
}
