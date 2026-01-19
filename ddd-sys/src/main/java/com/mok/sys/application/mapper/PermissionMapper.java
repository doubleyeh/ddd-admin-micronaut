package com.mok.sys.application.mapper;

import com.mok.sys.application.dto.permission.PermissionDTO;
import com.mok.sys.domain.model.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "jsr330", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PermissionMapper {

    @Mapping(source = "menu.id", target = "menuId")
    PermissionDTO toDto(Permission entity);

    default List<PermissionDTO> toDtoList(Collection<Permission> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream().map(this::toDto).toList();
    }
}
