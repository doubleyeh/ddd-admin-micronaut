package com.mok.application.sys.mapper;

import com.mok.application.sys.dto.role.RoleDTO;
import com.mok.application.sys.dto.role.RoleOptionDTO;
import com.mok.domain.sys.model.Role;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "jsr330", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {PermissionMapper.class,
        MenuMapper.class})
public interface RoleMapper {

    RoleDTO toDto(Role entity);

    RoleOptionDTO toOptionsDto(Role entity);

}
