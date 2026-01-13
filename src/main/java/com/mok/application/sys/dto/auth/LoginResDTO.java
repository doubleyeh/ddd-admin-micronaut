package com.mok.application.sys.dto.auth;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Serdeable
public class LoginResDTO {
    private String token;
    private String username;
    private String tenantId;
    private boolean isSuperTenant;
}