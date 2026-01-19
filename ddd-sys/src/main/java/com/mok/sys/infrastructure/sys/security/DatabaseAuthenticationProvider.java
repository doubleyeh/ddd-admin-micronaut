package com.mok.sys.infrastructure.sys.security;

import com.mok.sys.domain.model.Role;
import com.mok.sys.domain.repository.UserRepository;
import com.mok.common.infrastructure.util.SysUtil;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.HttpRequestAuthenticationProvider;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class DatabaseAuthenticationProvider<T> implements HttpRequestAuthenticationProvider<T> {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ACCOUNT_NOT_FOUND_OR_PASSWORD_ERROR = "用户不存在或密码错误";

    public DatabaseAuthenticationProvider(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AuthenticationResponse authenticate(HttpRequest<T> requestContext,
                                               AuthenticationRequest<String, String> authRequest) {

        String username = authRequest.getIdentity();
        String password = authRequest.getSecret();

        return userRepository.findByUsername(username)
                .map(user -> {
                    if (Integer.valueOf(0).equals(user.getState())) {
                        return AuthenticationResponse.failure("账户已禁用");
                    }
                    if (passwordEncoder.matches(password, user.getPassword())) {
                        Map<String, Object> attributes = new HashMap<>();
                        attributes.put("tenantId", user.getTenantId());
                        attributes.put("userId", user.getId());
                        attributes.put("roleIds", user.getRoles().stream().map(Role::getId).collect(Collectors.toSet()));
                        attributes.put("isSuperAdmin", SysUtil.isSuperAdmin(user.getTenantId(), user.getUsername()));
                        return AuthenticationResponse.success(username, attributes);
                    }
                    return AuthenticationResponse.failure(ACCOUNT_NOT_FOUND_OR_PASSWORD_ERROR);
                })
                .orElse(AuthenticationResponse.failure(ACCOUNT_NOT_FOUND_OR_PASSWORD_ERROR));
    }
}
