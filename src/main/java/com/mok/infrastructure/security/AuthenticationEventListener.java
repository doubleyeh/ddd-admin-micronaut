package com.mok.infrastructure.security;

import com.mok.application.sys.dto.auth.LoginRequest;
import com.mok.application.sys.service.LoginLogService;
import com.mok.domain.sys.model.LoginLog;
import com.mok.infrastructure.util.SysUtil;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.event.LoginFailedEvent;
import io.micronaut.security.event.LoginSuccessfulEvent;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Singleton
@RequiredArgsConstructor
public class AuthenticationEventListener implements ApplicationEventListener<Object> {

    private final LoginLogService loginLogService;

    @Override
    public void onApplicationEvent(Object event) {
        if (event instanceof LoginSuccessfulEvent) {
            handleLoginSuccess((LoginSuccessfulEvent) event);
        } else if (event instanceof LoginFailedEvent) {
            handleLoginFailure((LoginFailedEvent) event);
        }
    }

    private void handleLoginSuccess(LoginSuccessfulEvent event) {
        Authentication authentication = (Authentication) event.getSource();
        String username = authentication.getName();
        String tenantId = (String) authentication.getAttributes().get("tenantId");
        String ipAddress = getIpAddress();

        LoginLog loginLog = LoginLog.create(username, ipAddress, "SUCCESS", "Login successful");
        loginLog.assignTenant(tenantId);
        loginLogService.createLoginLog(loginLog);
    }

    private void handleLoginFailure(LoginFailedEvent event) {
        String username = "Unknown";
        String tenantId = "Unknown";
        String message = "Login failed";

        Optional<HttpRequest<Object>> requestOpt = ServerRequestContext.currentRequest();
        if (requestOpt.isPresent()) {
            Optional<LoginRequest> body = requestOpt.get().getBody(LoginRequest.class);
            if (body.isPresent()) {
                username = body.get().getUsername();
                tenantId = body.get().getTenantId();
            }
        }

        Object source = event.getSource();
        if (source instanceof AuthenticationResponse) {
            message = ((AuthenticationResponse) source).getMessage().orElse(message);
        }

        LoginLog loginLog = LoginLog.create(username, getIpAddress(), "FAILURE", message);
        loginLog.assignTenant(tenantId);
        loginLogService.createLoginLog(loginLog);
    }

    private String getIpAddress() {
        return ServerRequestContext.currentRequest()
                .map(SysUtil::getIpAddress)
                .orElse("Unknown");
    }
}