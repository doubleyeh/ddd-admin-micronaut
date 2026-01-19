package com.mok.sys.application.dto.tenantPackage;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Serdeable
public class TenantPackageOptionDTO {
    private Long id;
    private String name;
}
