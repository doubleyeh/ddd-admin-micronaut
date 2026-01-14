package com.mok.application.sys.service;

import com.mok.application.sys.dto.log.OperLogDTO;
import com.mok.application.sys.dto.log.OperLogQuery;
import com.mok.application.sys.mapper.OperLogMapper;
import com.mok.domain.sys.model.OperLog;
import com.mok.domain.sys.repository.OperLogRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class OperLogService {

    private final OperLogRepository operLogRepository;
    private final OperLogMapper operLogMapper;


    @Transactional(readOnly = true)
    public Page<OperLogDTO> findPage(OperLogQuery query, Pageable pageable) {
        Page<OperLog> entityPage = operLogRepository.findAll(query.toPredicate(), pageable);
        return entityPage.map(operLogMapper::toDto);
    }
}
