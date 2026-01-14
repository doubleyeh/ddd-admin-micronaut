package com.mok.infrastructure.log;


import com.mok.domain.sys.model.OperLog;
import com.mok.domain.sys.repository.OperLogRepository;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class OperLogListener {

    private final OperLogRepository operLogRepository;

    @Async
    @EventListener
    public void recordOperLog(OperLogEvent event) {
        OperLog operLog = event.getOperLog();
        try {
            operLogRepository.save(operLog);
            log.info("Remote operation log recorded: {}", operLog.getTitle());
        } catch (Exception e) {
            log.error("Failed to record operation log", e);
        }
    }
}
