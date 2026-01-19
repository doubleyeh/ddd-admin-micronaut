package com.mok.sys.application.dto.role;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.util.Set;

@Data
@Serdeable
public class RoleGrantDTO {
    private Set<Long> menuIds;
    private Set<Long> permissionIds;
}