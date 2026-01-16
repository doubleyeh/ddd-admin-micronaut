package com.mok.application.sys.service;

import com.mok.application.sys.dto.log.OperLogDTO;
import com.mok.application.sys.dto.log.OperLogQuery;
import com.mok.application.sys.mapper.OperLogMapper;
import com.mok.domain.sys.model.OperLog;
import com.mok.domain.sys.repository.OperLogRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OperLogServiceTest {

    private OperLogRepository operLogRepository;
    private OperLogMapper operLogMapper;
    private OperLogService operLogService;

    @BeforeEach
    void setUp() {
        operLogRepository = mock(OperLogRepository.class);
        operLogMapper = mock(OperLogMapper.class);
        operLogService = new OperLogService(operLogRepository, operLogMapper);
    }

    @Test
    void findPage_Success() {
        OperLogQuery query = new OperLogQuery();
        Pageable pageable = Pageable.from(0, 10);
        
        OperLog operLog = mock(OperLog.class);
        OperLogDTO operLogDTO = mock(OperLogDTO.class);

        Page<OperLog> entityPage = Page.of(Collections.singletonList(operLog), pageable, 1L);
        Page<OperLogDTO> dtoPage = Page.of(Collections.singletonList(operLogDTO), pageable, 1L);

        when(operLogRepository.findAll(any(PredicateSpecification.class), eq(pageable))).thenReturn(entityPage);
        when(operLogMapper.toDto(operLog)).thenReturn(operLogDTO);

        Page<OperLogDTO> result = operLogService.findPage(query, pageable);

        assertEquals(dtoPage.getContent(), result.getContent());
        assertEquals(dtoPage.getTotalSize(), result.getTotalSize());
        verify(operLogRepository).findAll(any(PredicateSpecification.class), eq(pageable));
        verify(operLogMapper).toDto(operLog);
    }
}
