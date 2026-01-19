package com.mok.sys.application.mapper;

import com.mok.sys.application.dto.menu.MenuDTO;
import com.mok.sys.domain.model.Menu;
import com.mok.sys.domain.model.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "jsr330", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuMapper {

    @Mapping(target = "permissionIds", source = "permissions", qualifiedByName = "permsToIds")
    @Mapping(source = "parent.id", target = "parentId")
    MenuDTO toDto(Menu entity);

    default List<MenuDTO> toDtoList(Collection<Menu> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream().map(this::toDto).toList();
    }

    @Named("permsToIds")
    default Set<Long> permsToIds(Set<Permission> permissions) {
        if (permissions == null) {
            return Collections.emptySet();
        }
        return permissions.stream().map(Permission::getId).collect(Collectors.toSet());
    }
}
