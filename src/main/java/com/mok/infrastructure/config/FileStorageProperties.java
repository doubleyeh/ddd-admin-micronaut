package com.mok.infrastructure.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties("file.storage.local")
public class FileStorageProperties {

    /**
     * Local storage path
     */
    private String path = "./uploads";

    /**
     * Domain for accessing files
     */
    private String domain = "http://localhost:8080";

    /**
     * URL prefix for files
     */
    private String prefix = "/files";
}
