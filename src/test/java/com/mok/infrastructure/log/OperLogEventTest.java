package com.mok.infrastructure.log;

import com.mok.domain.sys.model.OperLog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperLogEventTest {

    @Test
    void constructor_SetsSource() {
        OperLog operLog = OperLog.create("test", 1, "method", "GET", "user", "/url", "ip", "param", "result", 1, null, 100L);
        OperLogEvent event = new OperLogEvent(operLog);

        assertEquals(operLog, event.getSource());
        assertEquals(operLog, event.getOperLog());
    }

    @Test
    void getOperLog_ReturnsCorrectLog() {
        OperLog operLog = OperLog.create("test", 1, "method", "GET", "user", "/url", "ip", "param", "result", 1, null, 100L);
        OperLogEvent event = new OperLogEvent(operLog);

        OperLog retrieved = event.getOperLog();

        assertEquals(operLog, retrieved);
        assertNotNull(retrieved);
    }
}