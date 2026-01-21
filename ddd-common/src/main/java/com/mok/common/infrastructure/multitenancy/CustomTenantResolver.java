package com.mok.common.infrastructure.multitenancy;

import com.mok.common.infrastructure.util.SysUtil;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.multitenancy.exceptions.TenantNotFoundException;
import io.micronaut.multitenancy.tenantresolver.TenantResolver;
import io.micronaut.security.utils.SecurityService;
import jakarta.inject.Singleton;

import java.io.Serializable;
import java.util.Optional;

@Singleton
@Primary
public class CustomTenantResolver implements TenantResolver {

    private final SecurityService securityService;
    private final String tenantIdAttribute;

    public CustomTenantResolver(SecurityService securityService,
                                @Property(name = "micronaut.security.authentication.attributes.tenant-id", defaultValue = "tenantId") String tenantIdAttribute) {
        this.securityService = securityService;
        this.tenantIdAttribute = tenantIdAttribute;
    }

    @Override
    @NonNull
    @Deprecated
    public Serializable resolveTenantIdentifier() throws TenantNotFoundException {
        return resolveTenantId();
    }

    @Override
    public String resolveTenantId() throws TenantNotFoundException {
        Optional<String> resolvedTenantId = securityService.getAuthentication()
                .flatMap(authentication -> {
                    String username = authentication.getName();
                    Optional<String> tenantIdOptional = Optional.ofNullable(authentication.getAttributes().get(tenantIdAttribute))
                            .map(String.class::cast);

                    if (SysUtil.isSuperAdmin(tenantIdOptional.orElse(null), username)) {
                        return Optional.empty();
                    }
                    return tenantIdOptional;
                });

        return resolvedTenantId.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
    }
}
