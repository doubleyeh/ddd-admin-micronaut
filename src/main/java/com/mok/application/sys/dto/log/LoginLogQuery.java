package com.mok.application.sys.dto.log;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Serdeable
public class LoginLogQuery {

    private String username;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // TODO: QueryDSL Predicate logic needs to be adapted or moved to repository layer if QueryDSL is not used directly in DTOs
}
