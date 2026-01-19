package com.mok.sys.application.dto.tenant;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable
public class TenantCreateResultDTO extends TenantDTO {
    private String initialAdminPassword;
}