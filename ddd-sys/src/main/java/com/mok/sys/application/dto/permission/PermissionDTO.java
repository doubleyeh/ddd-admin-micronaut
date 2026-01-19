package com.mok.sys.application.dto.permission;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Serdeable
public class PermissionDTO {
    private Long id;
    private Long menuId;
    private String name;
    private String code;
    private String url;
    private String method;
    private String description;
}