package com.mok.web.sys;

import com.mok.application.sys.dto.log.LoginLogDTO;
import com.mok.application.sys.dto.log.LoginLogQuery;
import com.mok.application.sys.service.LoginLogService;
import com.mok.web.common.RestResponse;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginLogControllerTest {

    private LoginLogService loginLogService;
    private LoginLogController loginLogController;

    @BeforeEach
    void setUp() {
        loginLogService = mock(LoginLogService.class);
        loginLogController = new LoginLogController(loginLogService);
    }

    @Test
    void findPage() {
        LoginLogQuery query = new LoginLogQuery();
        query.setUsername("testUser");
        Pageable pageable = Pageable.from(0, 10);
        Page<LoginLogDTO> page = Page.of(Collections.emptyList(), pageable, 0L);

        when(loginLogService.findPage(query, pageable)).thenReturn(page);

        RestResponse<Page<LoginLogDTO>> response = loginLogController.findPage(query, pageable);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(page, response.getData());

        verify(loginLogService).findPage(query, pageable);
    }
}
