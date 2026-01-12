package com.mok.web.sys;

import com.mok.domain.sys.model.User;
import com.mok.domain.sys.repository.UserRepository;
import com.mok.infrastructure.common.Const;
import com.mok.infrastructure.tenant.TenantContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.micronaut.security.token.render.BearerAccessRefreshToken;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@MicronautTest(environments = "test")
public class LoginTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @BeforeEach
    void setup() {
        ScopedValue.where(TenantContext.TENANT_ID, Const.SUPER_TENANT_ID).run(() -> {
            userRepository.deleteAll();
            User user = User.create("admin", "123456", "Administrator", true);
            user.setTenantId(Const.SUPER_TENANT_ID);
            userRepository.save(user);
        });
    }

    @Test
    void testLogin() {
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials("admin", "123456");
        BearerAccessRefreshToken response = client.toBlocking()
                .retrieve(HttpRequest.POST("/login", creds), BearerAccessRefreshToken.class);

        Assertions.assertNotNull(response.getAccessToken());
        Assertions.assertEquals("admin", response.getUsername());
    }

    @Test
    void testLoginFailed() {
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials("admin", "wrong_password");

        io.micronaut.http.client.exceptions.HttpClientResponseException exception = Assertions.assertThrows(
                io.micronaut.http.client.exceptions.HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.POST("/login", creds))
        );

        Assertions.assertEquals(io.micronaut.http.HttpStatus.UNAUTHORIZED, exception.getStatus());
    }
}