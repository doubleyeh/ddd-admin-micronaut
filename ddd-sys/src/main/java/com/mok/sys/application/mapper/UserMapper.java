package com.mok.sys.application.mapper;

import com.mok.sys.application.dto.user.UserDTO;
import com.mok.sys.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "jsr330", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {RoleMapper.class})
public interface UserMapper {
    // 继承字段必须明确指出
    @Mapping(target = "createTime", source = "createTime")
    @Mapping(target = "roles", source = "roles")
    UserDTO toDto(User entity);
}
