package com.mok.application.sys.dto.user;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
@Serdeable
public class UserQuery {
    private String username;
    private String nickname;
    private Integer state;
    private String tenantId;

    private Long roleId;

    // TODO: QueryDSL Predicate logic needs to be adapted or moved to repository layer if QueryDSL is not used directly in DTOs
}