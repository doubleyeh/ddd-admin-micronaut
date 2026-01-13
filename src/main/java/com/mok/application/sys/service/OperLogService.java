package com.mok.application.sys.service;

import com.mok.domain.sys.repository.OperLogRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class OperLogService {

    private final OperLogRepository operLogRepository;

    // QueryDSL dependent method findPage is removed.
    // A replacement method should be implemented if pagination is needed.
}
