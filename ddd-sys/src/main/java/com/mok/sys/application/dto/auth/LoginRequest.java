package com.mok.sys.application.dto.auth;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Serdeable
public class LoginRequest {
    @NotBlank(message = "登录用户不允许为空")
    private String username;

    @NotBlank(message = "登录密码不允许为空")
    private String password;

    @NotBlank(message = "租户不允许为空")
    private String tenantId;
}