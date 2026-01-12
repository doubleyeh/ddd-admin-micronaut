package com.mok.domain.sys.model;

import com.mok.domain.common.BaseEntity;
import io.micronaut.core.annotation.Introspected;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "sys_tenant")
@Getter
@Setter
@Introspected
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String tenantId;
    @Column(nullable = false)
    private String name;
    private String contactPerson;
    private String contactPhone;
    private Integer state;
    private Long packageId;
}