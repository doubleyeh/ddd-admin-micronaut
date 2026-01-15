package com.mok.application.sys.dto.tenant;

import com.mok.infrastructure.common.Const;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.util.Objects;

@Data
@Serdeable
public class TenantDTO {
    private Long id;
    private String tenantId;
    private String name;
    private String contactPerson;
    private String contactPhone;
    private Integer state;
    private Long packageId;
    private String packageName;

    public boolean isEnabled() {
        return Objects.equals(Const.TenantState.NORMAL, state);
    }
}
