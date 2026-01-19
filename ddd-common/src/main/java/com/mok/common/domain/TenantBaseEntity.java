package com.mok.common.domain;

import com.mok.common.infrastructure.tenant.TenantContextHolder;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.util.StringUtils;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;


@MappedSuperclass
@Getter
@Setter
@Introspected
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantBaseEntity extends BaseEntity {
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @PrePersist
    protected void onCreate() {
        if (StringUtils.isEmpty(this.tenantId)) {
            this.tenantId = TenantContextHolder.getTenantId();
        }
    }
}
