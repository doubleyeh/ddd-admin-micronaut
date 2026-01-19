package com.mok.sys.application.mapper;

import com.mok.sys.application.dto.role.RoleDTO;
import com.mok.sys.application.dto.role.RoleOptionDTO;
import com.mok.sys.domain.model.Role;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "jsr330", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {PermissionMapper.class,
        MenuMapper.class})
public interface RoleMapper {

    RoleDTO toDto(Role entity);

    RoleOptionDTO toOptionsDto(Role entity);

}
