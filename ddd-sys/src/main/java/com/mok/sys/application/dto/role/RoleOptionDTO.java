package com.mok.sys.application.dto.role;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Serdeable
public class RoleOptionDTO {
    private Long id;
    private String name;
}
