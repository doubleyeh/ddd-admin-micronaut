package com.mok.sys.application.dto.log;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Serdeable
public class LoginLogDTO {
    private Long id;
    private String username;
    private String ipAddress;
    private String status;
    private String message;
    private String tenantId;
    private String tenantName;
    private LocalDateTime createTime;
}
