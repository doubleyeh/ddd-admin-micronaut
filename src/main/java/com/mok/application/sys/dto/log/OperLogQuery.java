package com.mok.application.sys.dto.log;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Serdeable
public class OperLogQuery {

    private String title;
    private String operName;
    private Integer status;

    // TODO: QueryDSL Predicate logic needs to be adapted or moved to repository layer if QueryDSL is not used directly in DTOs
}
