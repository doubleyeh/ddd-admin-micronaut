package com.mok.sys.application.listener;

import com.mok.sys.application.dto.user.UserPostDTO;
import com.mok.sys.application.event.TenantCreatedEvent;
import com.mok.sys.application.service.UserService;
import com.mok.common.infrastructure.common.Const;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class UserCreationListener {

    private final UserService userService;

    @EventListener
    public void onTenantCreated(TenantCreatedEvent event) {
        String tenantId = event.getTenantId();
        String tenantName = event.getTenantName();
        String rawPassword = event.getRawPassword();

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername(Const.DEFAULT_ADMIN_USERNAME);
        userPostDTO.setNickname(tenantName + "管理员");
        userPostDTO.setPassword(rawPassword);
        userPostDTO.setState(Const.UserState.NORMAL);
        userPostDTO.setTenantId(tenantId);

        userService.createForTenant(userPostDTO);
    }
}
