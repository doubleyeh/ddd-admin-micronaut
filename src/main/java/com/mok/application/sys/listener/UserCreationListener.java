package com.mok.application.sys.listener;

import com.mok.application.sys.dto.user.UserPostDTO;
import com.mok.application.sys.event.TenantCreatedEvent;
import com.mok.application.sys.service.UserService;
import com.mok.infrastructure.common.Const;
import com.mok.domain.sys.model.Tenant;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class UserCreationListener {

    private final UserService userService;

    @EventListener
    public void onTenantCreated(TenantCreatedEvent event) {
        Tenant tenant = event.getTenant();
        String rawPassword = event.getRawPassword();

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername(Const.DEFAULT_ADMIN_USERNAME);
        userPostDTO.setNickname(tenant.getName() + "管理员");
        userPostDTO.setPassword(rawPassword);
        userPostDTO.setState(Const.UserState.NORMAL);
        userPostDTO.setTenantId(tenant.getTenantId());

        userService.createForTenant(userPostDTO);
    }
}
