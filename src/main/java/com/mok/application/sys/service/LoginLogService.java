package com.mok.application.sys.service;

import com.mok.application.sys.dto.log.LoginLogDTO;
import com.mok.application.sys.dto.log.LoginLogQuery;
import com.mok.domain.sys.model.LoginLog;
import com.mok.domain.sys.repository.LoginLogRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
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

    @Transactional(readOnly = true)
    public Page<LoginLogDTO> findPage(LoginLogQuery query, Pageable pageable) {
        return loginLogRepository.findLoginLogPage(query.toPredicate(), pageable);
    }
}
