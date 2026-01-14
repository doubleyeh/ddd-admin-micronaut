package com.mok.infrastructure.log;


import com.mok.domain.sys.model.OperLog;
import io.micronaut.context.event.ApplicationEvent;

public class OperLogEvent extends ApplicationEvent {
    public OperLogEvent(OperLog operLog) {
        super(operLog);
    }

    public OperLog getOperLog() {
        return (OperLog) getSource();
    }
}
