package com.mok.common.infrastructure.multitenancy;

import com.mok.common.domain.BaseEntity;
import com.mok.common.domain.TenantBaseEntity;
import io.micronaut.data.event.listeners.PrePersistEventListener;
import io.micronaut.data.event.listeners.PreUpdateEventListener;
import io.micronaut.data.model.runtime.RuntimePersistentEntity;
import io.micronaut.security.utils.SecurityService;
import jakarta.inject.Singleton;
import io.micronaut.multitenancy.tenantresolver.TenantResolver;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.time.LocalDateTime;

@Singleton
public class TenantEntityListener implements PrePersistEventListener<Object>, PreUpdateEventListener<Object> {

    private final SecurityService securityService;
    private final TenantResolver tenantResolver;

    public TenantEntityListener(SecurityService securityService, TenantResolver tenantResolver) {
        this.securityService = securityService;
        this.tenantResolver = tenantResolver;
    }

    @Override
    public boolean prePersist(Object entity) {
        String username = securityService.username().orElse("system");
        LocalDateTime now = LocalDateTime.now();

        if (entity instanceof BaseEntity baseEntity) {
            baseEntity.setCreateTime(now);
            baseEntity.setCreateBy(username);
            baseEntity.setUpdateTime(now);
            baseEntity.setUpdateBy(username);
        }

        if (entity instanceof TenantBaseEntity tenantBaseEntity) {
            Serializable tenantId = tenantResolver.resolveTenantId();
            if (tenantId != null) {
                tenantBaseEntity.setTenantId(tenantId.toString());
            }
        }
        return true;
    }

    @Override
    public boolean preUpdate(Object entity) {
        String username = securityService.username().orElse("system");
        if (entity instanceof BaseEntity baseEntity) {
            baseEntity.setUpdateTime(LocalDateTime.now());
            baseEntity.setUpdateBy(username);
        }
        return true;
    }

    @Override
    public boolean supports(RuntimePersistentEntity<Object> entity, Class<? extends Annotation> eventType) {
        return eventType == PrePersist.class || eventType == PreUpdate.class;
    }
}
