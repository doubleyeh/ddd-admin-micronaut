package com.mok.application.sys.dto.tenantPackage;

import com.mok.application.sys.dto.menu.MenuDTO;
import com.mok.application.sys.dto.permission.PermissionDTO;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.util.Set;

@Data
@Serdeable
public class TenantPackageDTO {
    private Long id;
    private String name;
    private String description;
    private Integer state;
    private Set<MenuDTO> menus;
    private Set<PermissionDTO> permissions;
}
