package com.mok.application.sys.dto.role;

import com.mok.application.sys.dto.menu.MenuDTO;
import com.mok.application.sys.dto.permission.PermissionDTO;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Serdeable
public class RoleDTO {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer sort;
    private Integer state;
    private String tenantId;
    private String tenantName;
    private LocalDateTime createTime;
    private Set<MenuDTO> menus;
    private Set<PermissionDTO> permissions;
}
