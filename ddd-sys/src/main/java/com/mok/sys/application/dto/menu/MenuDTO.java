package com.mok.sys.application.dto.menu;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Serdeable
public class MenuDTO {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Boolean isHidden;
    private List<MenuDTO> children;
    private Set<Long> permissionIds;
}