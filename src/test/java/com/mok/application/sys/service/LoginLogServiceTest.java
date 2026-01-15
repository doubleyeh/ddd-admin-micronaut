package com.mok.application.sys.service;

import com.mok.application.sys.dto.log.LoginLogDTO;
import com.mok.application.sys.dto.log.LoginLogQuery;
import com.mok.domain.sys.model.LoginLog;
import com.mok.domain.sys.repository.LoginLogRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LoginLogServiceTest {

    private LoginLogRepository loginLogRepository;
    private LoginLogService loginLogService;

    @BeforeEach
    void setUp() {
        loginLogRepository = mock(LoginLogRepository.class);
        loginLogService = new LoginLogService(loginLogRepository);
    }

    @Test
    void createLoginLog_Success() {
        LoginLog loginLog = LoginLog.create("user1", "127.0.0.1", "SUCCESS", "Login successful");

        loginLogService.createLoginLog(loginLog);

        ArgumentCaptor<LoginLog> captor = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogRepository).save(captor.capture());
        LoginLog savedLog = captor.getValue();

        assertEquals("user1", savedLog.getUsername());
        assertEquals("SUCCESS", savedLog.getStatus());
    }

    @Test
    void findPage_Success() {
        LoginLogQuery query = new LoginLogQuery();
        Pageable pageable = Pageable.from(0, 10);
        Page<LoginLogDTO> page = Page.of(Collections.emptyList(), pageable, 0L);

        when(loginLogRepository.findLoginLogPage(any(), eq(pageable))).thenReturn(page);

        Page<LoginLogDTO> result = loginLogService.findPage(query, pageable);

        assertEquals(page, result);
        verify(loginLogRepository).findLoginLogPage(any(), eq(pageable));
    }
}
