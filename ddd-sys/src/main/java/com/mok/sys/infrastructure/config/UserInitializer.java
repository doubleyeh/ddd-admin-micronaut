package com.mok.sys.infrastructure.config;

import com.mok.sys.domain.model.User;
import com.mok.sys.domain.repository.UserRepository;
import com.mok.common.infrastructure.common.Const;
import com.mok.sys.infrastructure.sys.security.PasswordEncoder;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class UserInitializer implements ApplicationEventListener<StartupEvent> {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void onApplicationEvent(StartupEvent event) {
        if (userRepository.count() == 0) {
            String encodedPassword = passwordEncoder.encode("123456");
            User rootUser = User.create(Const.SUPER_ADMIN_USERNAME, encodedPassword, "超级管理员", true);
            rootUser.assignTenant(Const.SUPER_TENANT_ID);
            userRepository.save(rootUser);
        }
    }
}
