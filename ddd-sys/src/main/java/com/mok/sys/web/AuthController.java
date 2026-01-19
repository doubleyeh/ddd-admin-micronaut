package com.mok.sys.web;

import com.mok.sys.application.dto.auth.LoginRequest;
import com.mok.sys.application.dto.auth.LoginResDTO;
import com.mok.sys.infrastructure.security.CustomUserDetail;
import com.mok.sys.infrastructure.security.JwtTokenProvider;
import com.mok.common.infrastructure.util.SysUtil;
import com.mok.common.web.RestResponse;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.Authenticator;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller("/api/auth")
public class AuthController {

    private final Authenticator<HttpRequest<?>> authenticator;
    private final JwtTokenProvider tokenProvider;

    @Post("/login")
    public Mono<RestResponse<LoginResDTO>> authenticateUser(@Valid @Body LoginRequest loginRequest, HttpRequest<?> request) {
        String tenantId = loginRequest.getTenantId();
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        String ipAddress = SysUtil.getIpAddress(request);

        return Mono.from(authenticator.authenticate(request, new UsernamePasswordCredentials(username, password)))
                .map(response -> {
                    if (!response.isAuthenticated() || response.getAuthentication().isEmpty()) {
                        return RestResponse.failure(response.getMessage().orElse("认证失败"));
                    }

                    Authentication authentication = response.getAuthentication().get();
                    CustomUserDetail userDetail = convertToCustomUserDetail(authentication, tenantId);

                    try {
                        String jwt = tokenProvider.createToken(
                                username,
                                tenantId,
                                userDetail,
                                ipAddress,
                                SysUtil.getBrowser(request.getHeaders().get("User-Agent")));

                        return RestResponse.success(new LoginResDTO(
                                jwt,
                                username,
                                tenantId,
                                SysUtil.isSuperTenant(tenantId)));
                    } catch (Exception e) {
                        return RestResponse.failure("令牌创建失败");
                    }
                });
    }

    private CustomUserDetail convertToCustomUserDetail(Authentication auth, String tenantId) {
        Map<String, Object> attrs = auth.getAttributes();

        Long userId = Optional.ofNullable(attrs.get("userId"))
                .map(o -> Long.valueOf(o.toString()))
                .orElse(null);

        Object roleIdsObj = attrs.get("roleIds");
        Set<Long> roleIds = null;
        if (roleIdsObj instanceof Collection<?> coll) {
            roleIds = coll.stream()
                    .map(o -> Long.valueOf(o.toString()))
                    .collect(Collectors.toSet());
        }

        boolean isSuperAdmin = Boolean.TRUE.equals(attrs.get("isSuperAdmin"));

        return new CustomUserDetail(
                userId,
                auth.getName(),
                null,
                tenantId,
                roleIds,
                isSuperAdmin,
                auth.getRoles()
        );
    }

    @Post("/logout")
    public RestResponse<Void> logout(HttpRequest<?> request) {
        String bearerToken = request.getHeaders().getAuthorization().orElse(null);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String jwt = bearerToken.substring(7);
            tokenProvider.removeToken(jwt);
        }
        return RestResponse.success();
    }
}
