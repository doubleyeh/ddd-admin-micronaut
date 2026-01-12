package com.mok.infrastructure.sys.security;

import com.mok.domain.sys.repository.UserRepository;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.HttpRequestAuthenticationProvider;
import jakarta.inject.Singleton;
import java.util.Collections;

@Singleton
public class DatabaseAuthenticationProvider<T> implements HttpRequestAuthenticationProvider<T> {

    private final UserRepository userRepository;

    private static final String ACCOUNT_NOT_FOUND_OR_PASSWORD_ERROR = "用户不存在或密码错误";

    public DatabaseAuthenticationProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
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
                    if (user.getPassword().equals(password)) {
                        return AuthenticationResponse.success(username,
                                Collections.singletonMap("tenantId", user.getTenantId()));
                    }
                    return AuthenticationResponse.failure(ACCOUNT_NOT_FOUND_OR_PASSWORD_ERROR);
                })
                .orElse(AuthenticationResponse.failure(ACCOUNT_NOT_FOUND_OR_PASSWORD_ERROR));
    }
}