package com.mok.sys.infrastructure.security;

import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.validator.TokenValidator;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Singleton
public class RedisTokenValidator implements TokenValidator<HttpRequest<?>> {

    private final JwtTokenProvider tokenProvider;

    public RedisTokenValidator(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Publisher<Authentication> validateToken(String token, HttpRequest<?> request) {
        return Mono.fromCallable(() -> {
            com.mok.common.infrastructure.security.TokenSessionDTO session = tokenProvider.getSession(token);
            if (session != null && session.getPrincipal() != null) {
                com.mok.common.infrastructure.security.CustomUserDetail user = session.getPrincipal();
                return Authentication.build(
                        user.username(),
                        Objects.isNull(user.roleIds()) ? Collections.emptyList() : user.roleIds().stream().map(String::valueOf).toList(),
                        Map.of(
                                "userId", user.userId(),
                                "tenantId", user.tenantId(),
                                "isSuperAdmin", user.isSuperAdmin()
                        )
                );
            }
            return null;
        });
    }
}