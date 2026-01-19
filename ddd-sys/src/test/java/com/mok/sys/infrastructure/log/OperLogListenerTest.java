package com.mok.sys.infrastructure.log;

import com.mok.sys.domain.model.OperLog;
import com.mok.sys.domain.repository.OperLogRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class OperLogListenerTest {

    private OperLogRepository operLogRepository = mock(OperLogRepository.class);
    private OperLogListener listener = new OperLogListener(operLogRepository);

    @Test
    void recordOperLog_Success() {
        OperLog operLog = OperLog.create("test", 1, "method", "GET", "user", "/url", "ip", "param", "result", 1, null, 100L);
        OperLogEvent event = new OperLogEvent(operLog);

        listener.recordOperLog(event);

        verify(operLogRepository).save(operLog);
    }

    @Test
    void recordOperLog_SaveFails() {
        OperLog operLog = OperLog.create("test", 1, "method", "GET", "user", "/url", "ip", "param", "result", 1, null, 100L);
        OperLogEvent event = new OperLogEvent(operLog);

        doThrow(new RuntimeException("save failed")).when(operLogRepository).save(operLog);

        listener.recordOperLog(event);

        verify(operLogRepository).save(operLog);
        // Exception is caught and logged, no re-throw
    }
}
