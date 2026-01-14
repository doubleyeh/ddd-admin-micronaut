package com.mok.web.sys;

import com.mok.application.sys.dto.role.*;
import com.mok.application.sys.service.RoleService;
import com.mok.infrastructure.log.BusinessType;
import com.mok.infrastructure.log.OperLogRecord;
import com.mok.web.common.RestResponse;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import java.util.List;


@Controller("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Get
    @Secured("hasAuthority('role:list')")
    public RestResponse<Page<@NonNull RoleDTO>> findPage(RoleQuery query, Pageable pageable) {
        Page<@NonNull RoleDTO> page = roleService.findPage(query, pageable);
        return RestResponse.success(page);
    }

    @Get("/{id}")
    @Secured("hasAuthority('role:list')")
    public RestResponse<RoleDTO> getById(@PathVariable Long id) {
        RoleDTO roleDTO = roleService.getById(id);
        return RestResponse.success(roleDTO);
    }

    @Post
    @Secured("hasAuthority('role:create')")
    @OperLogRecord(title = "角色管理", businessType = BusinessType.INSERT)
    public RestResponse<RoleDTO> save(@Body @Valid RoleSaveDTO roleSaveDTO) {
        RoleDTO savedRole = roleService.createRole(roleSaveDTO);
        return RestResponse.success(savedRole);
    }

    @Put("/{id}")
    @Secured("hasAuthority('role:update')")
    @OperLogRecord(title = "角色管理", businessType = BusinessType.UPDATE)
    public RestResponse<RoleDTO> update(@PathVariable Long id, @Body @Valid RoleSaveDTO roleSaveDTO) {
        roleSaveDTO.setId(id);
        RoleDTO updatedRole = roleService.updateRole(roleSaveDTO);
        return RestResponse.success(updatedRole);
    }

    @Put("/{id}/state")
    @Secured("hasAuthority('role:update')")
    @OperLogRecord(title = "角色管理", businessType = BusinessType.UPDATE)
    public RestResponse<RoleDTO> updateState(@PathVariable Long id, @Valid @NotNull @QueryValue Integer state) {
        RoleDTO dto = roleService.updateState(id, state);
        return RestResponse.success(dto);
    }

    @Delete("/{id}")
    @Secured("hasAuthority('role:delete')")
    @OperLogRecord(title = "角色管理", businessType = BusinessType.DELETE)
    public RestResponse<Void> deleteById(@PathVariable Long id) {
        roleService.deleteRoleBeforeValidation(id);
        return RestResponse.success();
    }

    @Post("/{id}/grant")
    @Secured("hasAuthority('role:update')")
    @OperLogRecord(title = "角色管理", businessType = BusinessType.GRANT)
    public RestResponse<Void> grant(@PathVariable Long id, @Body RoleGrantDTO grantDTO) {
        roleService.grant(id, grantDTO);
        return RestResponse.success();
    }

    @Get("/options")
    public RestResponse<List<RoleOptionDTO>> getRoleOptions(RoleQuery query) {
        List<RoleOptionDTO> options = roleService.getRoleOptions(query);
        return RestResponse.success(options);
    }
}
