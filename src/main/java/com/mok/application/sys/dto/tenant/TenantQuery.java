package com.mok.application.sys.dto.tenant;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
@Serdeable
public class TenantQuery  {
    private String tenantId;
    private String name;
    private Integer state;

    // TODO: QueryDSL Predicate logic needs to be adapted or moved to repository layer if QueryDSL is not used directly in DTOs
}
