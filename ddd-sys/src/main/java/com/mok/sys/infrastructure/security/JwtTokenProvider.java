package com.mok.sys.infrastructure.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mok.common.infrastructure.common.Const;
import com.mok.common.infrastructure.security.CustomUserDetail;
import com.mok.common.infrastructure.security.TokenSessionDTO;
import com.mok.common.infrastructure.security.TokenProvider;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanIterator;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider implements TokenProvider {

    @Value("${auth.expire-second:3600}")
    private long jwtExpireSecond;

    @Value("${auth.allow-multi-device:true}")
    private boolean allowMultiDevice;

    private final RedisCommands<String, String> redisCommands;
    private final ObjectMapper objectMapper;

    @Override
    public String createToken(String username, String tenantId, CustomUserDetail principal, String ipAddress, String browser) throws JsonProcessingException {
        String userKey = Const.CacheKey.USER_TOKENS + tenantId + ":" + username;
        if (!allowMultiDevice) {
            String oldToken = redisCommands.get(userKey);
            if (StringUtils.isNotEmpty(oldToken)) {
                redisCommands.del(Const.CacheKey.AUTH_TOKEN + oldToken);
            }
        }

        String token = UUID.randomUUID().toString();
        String tokenKey = Const.CacheKey.AUTH_TOKEN + token;
        TokenSessionDTO session = new TokenSessionDTO(username, tenantId, principal, ipAddress, browser, System.currentTimeMillis());
        session.setToken(token);
        String sessionJson = objectMapper.writeValueAsString(session);

        redisCommands.setex(tokenKey, jwtExpireSecond, sessionJson);
        if (allowMultiDevice) {
            redisCommands.sadd(userKey, token);
        } else {
            redisCommands.set(userKey, token);
        }
        redisCommands.expire(userKey, jwtExpireSecond);

        return token;
    }

    @Override
    public TokenSessionDTO getSession(String token) {
        String tokenKey = Const.CacheKey.AUTH_TOKEN + token;
        String data = redisCommands.get(tokenKey);
        if (StringUtils.isNotEmpty(data)) {
            try {
                TokenSessionDTO session = objectMapper.readValue(data, TokenSessionDTO.class);
                if (session == null) {
                    return null;
                }

                Long expire = redisCommands.ttl(tokenKey);

                // 剩余时间小于10分钟，刷新
                if (expire != null && expire > 0 && expire < 600) {
                    redisCommands.expire(tokenKey, jwtExpireSecond);
                    String userKey = Const.CacheKey.USER_TOKENS + session.getTenantId() + ":" + session.getUsername();
                    redisCommands.expire(userKey, jwtExpireSecond);
                }
                return session;
            } catch (Exception e) {
                log.error("Failed to deserialize TokenSessionDTO from Redis", e);
                return null;
            }
        }
        return null;
    }

    public List<OnlineUserDTO> getAllOnlineUsers(Map<String, String> tenantMap, String currentTenantId, boolean isSuper) {
        Set<String> keys = new HashSet<>();
        ScanIterator<String> scanIterator = ScanIterator.scan(redisCommands, ScanArgs.Builder.matches(Const.CacheKey.AUTH_TOKEN + "*").limit(1000));
        while (scanIterator.hasNext()) {
            keys.add(scanIterator.next());
        }

        List<TokenSessionDTO> sessions = new ArrayList<>();
        if (!keys.isEmpty()) {
            for (String fullKey : keys) {
                String token = fullKey.replace(Const.CacheKey.AUTH_TOKEN, "");
                TokenSessionDTO session = getSession(token);
                if (session != null) {
                    session.setToken(token);
                    sessions.add(session);
                }
            }
        }

        Map<String, List<TokenSessionDTO>> grouped = sessions.stream()
                .filter(s -> s.getPrincipal() != null)
                .filter(s -> isSuper || s.getTenantId().equals(currentTenantId))
                .collect(Collectors.groupingBy(s -> s.getTenantId() + ":" + s.getPrincipal().userId()));

        return grouped.values().stream().map(userSessions -> {
            TokenSessionDTO first = userSessions.getFirst();
            List<OnlineUserDTO.SessionDetail> details = userSessions.stream()
                    .map(s -> new OnlineUserDTO.SessionDetail(
                            s.getToken(),
                            s.getIp(),
                            s.getBrowser(),
                            s.getLoginTime()
                    )).toList();

            return new OnlineUserDTO(
                    first.getPrincipal().userId(),
                    first.getUsername(),
                    first.getTenantId(),
                    tenantMap.getOrDefault(first.getTenantId(), first.getTenantId()),
                    details
            );
        }).toList();
    }

    public void removeToken(String token) {
        TokenSessionDTO session = getSession(token);
        if (session != null) {
            String tokenKey = Const.CacheKey.AUTH_TOKEN + token;
            String userKey = Const.CacheKey.USER_TOKENS + session.getTenantId() + ":" + session.getUsername();

            redisCommands.del(tokenKey);

            if (allowMultiDevice) {
                redisCommands.srem(userKey, token);
            } else {
                redisCommands.del(userKey);
            }
        }
    }
}
