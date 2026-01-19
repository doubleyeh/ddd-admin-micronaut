package com.mok.sys.application.dto.tenantPackage;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Serdeable
public class TenantPackageSaveDTO {
    @NotBlank(message = "套餐名称不能为空")
    private String name;
    private String description;
    private Integer state;
}
