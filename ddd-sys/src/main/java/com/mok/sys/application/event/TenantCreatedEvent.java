package com.mok.sys.application.event;

import io.micronaut.context.event.ApplicationEvent;
import lombok.Getter;

@Getter
public class TenantCreatedEvent extends ApplicationEvent {

    private final String tenantId;
    private final String tenantName;
    private final String rawPassword;

    public TenantCreatedEvent(String tenantId, String tenantName, String rawPassword) {
        super(tenantId);
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.rawPassword = rawPassword;
    }
}
