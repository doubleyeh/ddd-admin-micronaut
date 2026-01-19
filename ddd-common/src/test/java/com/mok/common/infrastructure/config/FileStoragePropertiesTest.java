package com.mok.common.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileStoragePropertiesTest {

    @Test
    void testDefaultValues() {
        FileStorageProperties properties = new FileStorageProperties();

        assertEquals("./uploads", properties.getPath());
        assertEquals("http://localhost:8080", properties.getDomain());
        assertEquals("/files", properties.getPrefix());
    }

    @Test
    void testSettersAndGetters() throws Exception {
        FileStorageProperties properties = new FileStorageProperties();

        // Use reflection to set values since setters might not be public
        var pathField = properties.getClass().getDeclaredField("path");
        pathField.setAccessible(true);
        pathField.set(properties, "/custom/path");

        var domainField = properties.getClass().getDeclaredField("domain");
        domainField.setAccessible(true);
        domainField.set(properties, "https://example.com");

        var prefixField = properties.getClass().getDeclaredField("prefix");
        prefixField.setAccessible(true);
        prefixField.set(properties, "/custom/files");

        assertEquals("/custom/path", properties.getPath());
        assertEquals("https://example.com", properties.getDomain());
        assertEquals("/custom/files", properties.getPrefix());
    }
}