package com.mok.application.sys.event;

import com.mok.domain.sys.model.Tenant;
import lombok.Getter;

@Getter
public class TenantCreatedEvent {

    private final Tenant tenant;
    private final String rawPassword;

    public TenantCreatedEvent(Tenant tenant, String rawPassword) {
        this.tenant = tenant;
        this.rawPassword = rawPassword;
    }
}
