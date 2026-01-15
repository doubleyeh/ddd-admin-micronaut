package com.mok.infrastructure.sys.security;

import jakarta.inject.Singleton;

@Singleton
public interface PasswordEncoder {
    String encode(CharSequence rawPassword);

    boolean matches(CharSequence rawPassword, String encodedPassword);
}
