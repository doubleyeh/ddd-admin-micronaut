package com.mok.application.sys.dto.permission;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Serdeable
public class PermissionOptionDTO {
    private Long id;
    private String name;
    private Boolean isPermission = true;
}