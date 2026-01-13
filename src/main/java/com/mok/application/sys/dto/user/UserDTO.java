package com.mok.application.sys.dto.user;

import com.mok.application.sys.dto.role.RoleOptionDTO;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Serdeable
public class UserDTO {
    private Long id;
    private String username;
    private String nickname;
    private Integer state;
    private LocalDateTime createTime;

    private String tenantId;
    private String tenantName;

    private Boolean isTenantAdmin;

    private Set<RoleOptionDTO> roles;
}