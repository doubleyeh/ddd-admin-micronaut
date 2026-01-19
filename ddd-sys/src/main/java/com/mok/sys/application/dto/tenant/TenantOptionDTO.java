package com.mok.sys.application.dto.tenant;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Serdeable
public class TenantOptionDTO {
    private Long id;
    private String tenantId;
    private String name;
}