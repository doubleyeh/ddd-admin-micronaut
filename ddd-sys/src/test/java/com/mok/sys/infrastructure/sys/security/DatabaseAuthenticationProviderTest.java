package com.mok.sys.infrastructure.sys.security;

import com.mok.sys.domain.model.Role;
import com.mok.sys.domain.model.User;
import com.mok.sys.domain.repository.UserRepository;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DatabaseAuthenticationProviderTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private DatabaseAuthenticationProvider provider;
    private HttpRequest<?> request;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        provider = new DatabaseAuthenticationProvider(userRepository, passwordEncoder);
        request = mock(HttpRequest.class);
    }

    @Test
    void authenticate_UserExistsAndPasswordMatches_ReturnsSuccess() {
        String username = "testuser";
        String password = "password";
        String tenantId = "tenant1";
        Long userId = 1L;

        User user = mock(User.class);
        when(user.getState()).thenReturn(1); // Normal state
        when(user.getTenantId()).thenReturn(tenantId);
        when(user.getId()).thenReturn(userId);
        when(user.getUsername()).thenReturn(username);

        Role role = mock(Role.class);
        when(role.getId()).thenReturn(1L);
        when(user.getRoles()).thenReturn(Set.of(role));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);

        AuthenticationRequest<String, String> authRequest = new AuthenticationRequest<String, String>() {
            @Override
            public String getIdentity() {
                return username;
            }

            @Override
            public String getSecret() {
                return password;
            }
        };
        AuthenticationResponse response = provider.authenticate(request, authRequest);

        assertTrue(response.isAuthenticated());
        assertEquals(username, response.getAuthentication().get().getName());
        verify(passwordEncoder).matches(password, user.getPassword());
    }

    @Test
    void authenticate_UserExistsButDisabled_ReturnsFailure() {
        String username = "testuser";
        String password = "password";

        User user = mock(User.class);
        when(user.getState()).thenReturn(0); // Disabled state

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        AuthenticationRequest<String, String> authRequest = new AuthenticationRequest<String, String>() {
            @Override
            public String getIdentity() {
                return username;
            }

            @Override
            public String getSecret() {
                return password;
            }
        };
        AuthenticationResponse response = provider.authenticate(request, authRequest);

        assertFalse(response.isAuthenticated());
        assertEquals("账户已禁用", response.getMessage().get());
    }

    @Test
    void authenticate_UserExistsButPasswordNotMatches_ReturnsFailure() {
        String username = "testuser";
        String password = "password";

        User user = mock(User.class);
        when(user.getState()).thenReturn(1);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(false);

        AuthenticationRequest<String, String> authRequest = new AuthenticationRequest<String, String>() {
            @Override
            public String getIdentity() {
                return username;
            }

            @Override
            public String getSecret() {
                return password;
            }
        };
        AuthenticationResponse response = provider.authenticate(request, authRequest);

        assertFalse(response.isAuthenticated());
        assertEquals("用户不存在或密码错误", response.getMessage().get());
    }

    @Test
    void authenticate_UserNotFound_ReturnsFailure() {
        String username = "nonexistent";
        String password = "password";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        AuthenticationRequest<String, String> authRequest = new AuthenticationRequest<String, String>() {
            @Override
            public String getIdentity() {
                return username;
            }

            @Override
            public String getSecret() {
                return password;
            }
        };
        AuthenticationResponse response = provider.authenticate(request, authRequest);

        assertFalse(response.isAuthenticated());
        assertEquals("用户不存在或密码错误", response.getMessage().get());
    }
}