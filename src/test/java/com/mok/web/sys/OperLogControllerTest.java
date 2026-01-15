package com.mok.web.sys;

import com.mok.application.sys.dto.log.OperLogDTO;
import com.mok.application.sys.dto.log.OperLogQuery;
import com.mok.application.sys.service.OperLogService;
import com.mok.web.common.RestResponse;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperLogControllerTest {

    private OperLogService operLogService;
    private OperLogController operLogController;

    @BeforeEach
    void setUp() {
        operLogService = mock(OperLogService.class);
        operLogController = new OperLogController(operLogService);
    }

    @Test
    void findPage() {
        OperLogQuery query = new OperLogQuery();
        Pageable pageable = Pageable.from(0, 10);
        Page<OperLogDTO> page = Page.of(Collections.emptyList(), pageable, 0L);

        when(operLogService.findPage(query, pageable)).thenReturn(page);

        RestResponse<Page<OperLogDTO>> response = operLogController.findPage(query, pageable);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(page, response.getData());

        verify(operLogService).findPage(query, pageable);
    }
}