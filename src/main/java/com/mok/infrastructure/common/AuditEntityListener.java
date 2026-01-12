package com.mok.infrastructure.common;

import com.mok.domain.common.BaseEntity;
import io.micronaut.security.utils.SecurityService;
import jakarta.inject.Singleton;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Singleton
public class AuditEntityListener {
    private final SecurityService securityService;

    public AuditEntityListener(SecurityService securityService) {
        this.securityService = securityService;
    }

    @PrePersist
    public void setCreatedBy(Object entity) {
        if (entity instanceof BaseEntity base) {
            String user = securityService.username().orElse("system");
            base.setCreatedBy(user);
            base.setUpdatedBy(user);
        }
    }

    @PreUpdate
    public void setUpdatedBy(Object entity) {
        if (entity instanceof BaseEntity base) {
            base.setUpdatedBy(securityService.username().orElse("system"));
        }
    }
}