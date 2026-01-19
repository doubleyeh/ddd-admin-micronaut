package com.mok.sys.application.dto.menu;

import com.mok.sys.application.dto.permission.PermissionOptionDTO;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.util.List;

@Data
@Serdeable
public class MenuOptionDTO {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private List<MenuOptionDTO> children;
    private List<PermissionOptionDTO> permissions;
    private Boolean isPermission = false;
}