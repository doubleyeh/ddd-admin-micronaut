package com.mok.application.sys.dto.menu;

import com.mok.application.sys.dto.permission.PermissionOptionDTO;
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