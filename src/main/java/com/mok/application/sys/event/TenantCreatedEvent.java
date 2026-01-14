package com.mok.application.sys.event;

import com.mok.domain.sys.model.Tenant;
import io.micronaut.context.event.ApplicationEvent;
import lombok.Getter;

@Getter
public class TenantCreatedEvent extends ApplicationEvent {

    private final Tenant tenant;
    private final String rawPassword;

    public TenantCreatedEvent(Tenant tenant, String rawPassword) {
        super(tenant);
        this.tenant = tenant;
        this.rawPassword = rawPassword;
    }
}
