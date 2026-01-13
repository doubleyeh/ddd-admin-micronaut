package com.mok.application.sys.service;

import com.mok.domain.sys.model.LoginLog;
import com.mok.domain.sys.repository.LoginLogRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;

    @Transactional
    public void createLoginLog(LoginLog loginLog) {
        loginLogRepository.save(loginLog);
    }

    // QueryDSL dependent method findPage is removed.
    // A replacement method should be implemented if pagination is needed.
}
