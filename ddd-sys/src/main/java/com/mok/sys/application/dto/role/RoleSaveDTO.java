package com.mok.sys.application.dto.role;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
@Serdeable
public class RoleSaveDTO {
    private Long id;

    @NotBlank(message = "角色名称不能为空")
    private String name;

    @NotBlank(message = "角色编码不能为空")
    private String code;

    private String description;
    private Integer sort;
    private Integer state;

    private Set<Long> menuIds;
    private Set<Long> permissionIds;
}
