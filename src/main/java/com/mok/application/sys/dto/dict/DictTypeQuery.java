package com.mok.application.sys.dto.dict;

import io.micronaut.core.util.StringUtils;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Serdeable
public class DictTypeQuery {
    private String name;
    private String code;

    // TODO: QueryDSL Predicate logic needs to be adapted or moved to repository layer if QueryDSL is not used directly in DTOs
    // For now, we keep the fields.
}
