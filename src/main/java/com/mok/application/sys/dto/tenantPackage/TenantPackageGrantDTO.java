package com.mok.application.sys.dto.tenantPackage;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.util.Set;

@Data
@Serdeable
public class TenantPackageGrantDTO {
    private Set<Long> menuIds;
    private Set<Long> permissionIds;
}
