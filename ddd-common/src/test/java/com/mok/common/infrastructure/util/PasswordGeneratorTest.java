package com.mok.common.infrastructure.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordGeneratorTest {

    @Test
    public void testGenerateRandomPassword() {
        String password = PasswordGenerator.generateRandomPassword();
        assertEquals(8, password.length());

        String allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz0123456789";
        for (char c : password.toCharArray()) {
            assertTrue(allowedChars.indexOf(c) >= 0);
        }

        String password1 = PasswordGenerator.generateRandomPassword();
        String password2 = PasswordGenerator.generateRandomPassword();
        String password3 = PasswordGenerator.generateRandomPassword();

        assertFalse(password1.equals(password2) && password2.equals(password3));
    }
}