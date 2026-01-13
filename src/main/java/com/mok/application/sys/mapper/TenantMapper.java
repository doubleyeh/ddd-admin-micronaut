package com.mok.application.sys.mapper;

import com.mok.application.sys.dto.tenant.TenantCreateResultDTO;
import com.mok.application.sys.dto.tenant.TenantDTO;
import com.mok.application.sys.dto.tenant.TenantOptionDTO;
import com.mok.domain.sys.model.Tenant;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "jsr330", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMapper {

    TenantDTO toDto(Tenant entity);

    void updateEntityFromDto(Tenant entity, @MappingTarget TenantCreateResultDTO dto);

    List<TenantOptionDTO> dtoToOptionsDto(List<TenantDTO> dtoList);
}
