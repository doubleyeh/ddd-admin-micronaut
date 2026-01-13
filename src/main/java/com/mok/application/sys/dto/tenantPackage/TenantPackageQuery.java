package com.mok.application.sys.dto.tenantPackage;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Serdeable
public class TenantPackageQuery {
    private String name;
    private Integer state;

    // TODO: QueryDSL Predicate logic needs to be adapted or moved to repository layer if QueryDSL is not used directly in DTOs
}
