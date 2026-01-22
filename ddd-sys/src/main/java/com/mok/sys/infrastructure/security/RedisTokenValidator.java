package com.mok.sys.infrastructure.security;

import com.mok.common.infrastructure.common.Const;
import com.mok.common.infrastructure.security.CustomUserDetail;
import com.mok.common.infrastructure.security.TokenSessionDTO;
import com.mok.sys.application.service.PermissionService;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.validator.TokenValidator;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.*;

@Singleton
@RequiredArgsConstructor
public class RedisTokenValidator implements TokenValidator<HttpRequest<?>> {

    private final JwtTokenProvider tokenProvider;
    private final PermissionService permissionService;

    @Override
    public Publisher<Authentication> validateToken(String token, HttpRequest<?> request) {
        return Mono.fromCallable(() -> {
            TokenSessionDTO session = tokenProvider.getSession(token);
            if (session != null && session.getPrincipal() != null) {
                CustomUserDetail user = session.getPrincipal();

                List<String> authrizes = new ArrayList<>();
                if(user.isSuperAdmin()){
                    authrizes.add(Const.SUPER_ADMIN_ROLE_CODE);
                    authrizes.addAll(permissionService.getAllPermissionCodes());
                }else{
                    if(Objects.nonNull(user.roleIds()) && !user.roleIds().isEmpty()){
                        authrizes.addAll(permissionService.getPermissionsByRoleIds(user.roleIds()));
                    }
                }

                return Authentication.build(
                        user.username(),
                        authrizes,
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