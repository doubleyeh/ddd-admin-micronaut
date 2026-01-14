package com.mok.web.sys;

import com.mok.application.sys.dto.auth.LoginRequest;
import com.mok.application.sys.dto.auth.LoginResDTO;
import com.mok.infrastructure.security.CustomUserDetail;
import com.mok.infrastructure.security.JwtTokenProvider;
import com.mok.infrastructure.tenant.TenantContextHolder;
import com.mok.infrastructure.util.SysUtil;
import com.mok.web.common.RestResponse;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.authentication.Authentication;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider) {
            this.authenticationManager = authenticationManager;
            this.tokenProvider = tokenProvider;
    }

    @Post("/login")
    public RestResponse<LoginResDTO> authenticateUser(@Valid @Body LoginRequest loginRequest, HttpRequest<?> request) {
        String tenantId = loginRequest.getTenantId();
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        String ipAddress = SysUtil.getIpAddress(request);

        return ScopedValue.where(TenantContextHolder.TENANT_ID, tenantId)
                .where(TenantContextHolder.USERNAME, username)
                .call(() -> {
                    Authentication authentication = authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    password));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    String jwt = tokenProvider.createToken(
                            username,
                            tenantId, (CustomUserDetail)authentication.getPrincipal(), ipAddress, SysUtil.getBrowser(request.getHeader("User-Agent")));

                    return RestResponse.success(new LoginResDTO(
                            jwt,
                            username,
                            tenantId, SysUtil.isSuperTenant(tenantId)));
                });
    }

    @Post("/logout")
    public RestResponse<Void> logout(HttpRequest<?> request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String jwt = bearerToken.substring(7);
            tokenProvider.removeToken(jwt);
        }
        SecurityContextHolder.clearContext();
        return RestResponse.success();
    }
}