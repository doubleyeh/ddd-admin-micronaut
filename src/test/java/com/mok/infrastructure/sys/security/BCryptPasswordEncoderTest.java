package com.mok.infrastructure.sys.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordEncoderTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void encode_ReturnsNonNullHashedPassword() {
        String rawPassword = "password123";

        String hashed = encoder.encode(rawPassword);

        assertNotNull(hashed);
        assertNotEquals(rawPassword, hashed);
        assertTrue(hashed.startsWith("$2a$") || hashed.startsWith("$2b$") || hashed.startsWith("$2y$"));
    }

    @Test
    void matches_WithMatchingPassword_ReturnsTrue() {
        String rawPassword = "password123";

        String hashed = encoder.encode(rawPassword);
        boolean matches = encoder.matches(rawPassword, hashed);

        assertTrue(matches);
    }

    @Test
    void matches_WithNonMatchingPassword_ReturnsFalse() {
        String rawPassword = "password123";
        String wrongPassword = "wrongpassword";

        String hashed = encoder.encode(rawPassword);
        boolean matches = encoder.matches(wrongPassword, hashed);

        assertFalse(matches);
    }

    @Test
    void matches_WithNullRawPassword_ThrowsException() {
        String hashed = encoder.encode("password123");

        assertThrows(NullPointerException.class, () -> encoder.matches(null, hashed));
    }

    @Test
    void matches_WithNullHashedPassword_ThrowsException() {
        assertThrows(NullPointerException.class, () -> encoder.matches("password123", null));
    }

    @Test
    void encode_WithEmptyPassword() {
        String rawPassword = "";

        String hashed = encoder.encode(rawPassword);

        assertNotNull(hashed);
        assertTrue(encoder.matches(rawPassword, hashed));
    }
}