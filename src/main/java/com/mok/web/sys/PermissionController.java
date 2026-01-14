package com.mok.web.sys;

import com.mok.application.sys.dto.permission.PermissionDTO;
import com.mok.application.sys.dto.permission.PermissionQuery;
import com.mok.application.sys.service.PermissionService;
import com.mok.web.common.RestResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Controller("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Get("/menu/{menuId}")
    @Secured("hasRole('SUPER_ADMIN')")
    public RestResponse<List<PermissionDTO>> findByMenuId(@PathVariable Long menuId) {
        PermissionQuery query = new PermissionQuery();
        query.setMenuId(menuId);
        return RestResponse.success(permissionService.findAll(query));
    }

    @Post
    @Secured("hasRole('SUPER_ADMIN')")
    public RestResponse<PermissionDTO> save(@Body PermissionDTO dto) {
        return RestResponse.success(permissionService.createPermission(dto));
    }

    @Put("/{id}")
    @Secured("hasRole('SUPER_ADMIN')")
    public RestResponse<PermissionDTO> update(@PathVariable Long id, @Body PermissionDTO dto) {
        dto.setId(id);
        return RestResponse.success(permissionService.updatePermission(dto));
    }

    @Delete("/{id}")
    @Secured("hasRole('SUPER_ADMIN')")
    public RestResponse<Void> delete(@PathVariable Long id) {
        permissionService.deleteById(id);
        return RestResponse.success();
    }
}