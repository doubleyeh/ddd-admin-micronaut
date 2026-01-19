package com.mok.sys.infrastructure.log;


import com.mok.sys.domain.model.OperLog;
import io.micronaut.context.event.ApplicationEvent;

public class OperLogEvent extends ApplicationEvent {
    public OperLogEvent(OperLog operLog) {
        super(operLog);
    }

    public OperLog getOperLog() {
        return (OperLog) getSource();
    }
}
