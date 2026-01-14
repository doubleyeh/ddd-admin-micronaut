package com.mok.web.sys;

import com.mok.application.sys.dto.menu.MenuDTO;
import com.mok.application.sys.dto.menu.MenuOptionDTO;
import com.mok.application.sys.service.MenuService;
import com.mok.web.common.RestResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Controller("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Get("/tree")
    @Secured("hasRole('SUPER_ADMIN')")
    public RestResponse<List<MenuDTO>> getTree() {
        return RestResponse.success(menuService.buildMenuTree(menuService.findAll()));
    }

    @Post
    @Secured("hasRole('SUPER_ADMIN')")
    public RestResponse<MenuDTO> save(@Body MenuDTO dto) {
        return RestResponse.success(menuService.createMenu(dto));
    }

    @Put("/{id}")
    @Secured("hasRole('SUPER_ADMIN')")
    public RestResponse<MenuDTO> update(@PathVariable Long id, @Body MenuDTO dto) {
        dto.setId(id);
        return RestResponse.success(menuService.updateMenu(dto));
    }

    @Delete("/{id}")
    @Secured("hasRole('SUPER_ADMIN')")
    public RestResponse<Void> delete(@PathVariable Long id) {
        menuService.deleteById(id);
        return RestResponse.success();
    }

    @Get("/tree-options")
    public RestResponse<List<MenuOptionDTO>> getMenuTreeOptions() {
        return RestResponse.success(menuService.buildMenuAndPermissionTree());
    }
}