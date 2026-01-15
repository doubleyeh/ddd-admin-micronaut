package com.mok.web.sys;

import com.mok.application.sys.dto.tenant.TenantOptionDTO;
import com.mok.application.sys.service.TenantService;
import com.mok.infrastructure.log.BusinessType;
import com.mok.infrastructure.log.OperLogRecord;
import com.mok.infrastructure.security.JwtTokenProvider;
import com.mok.infrastructure.security.OnlineUserDTO;
import com.mok.infrastructure.tenant.TenantContextHolder;
import com.mok.web.common.RestResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller("/api/online-user")
public class OnlineUserController {

    private final JwtTokenProvider tokenProvider;
    private final TenantService tenantService;

    @Get("/list")
    @Secured("hasAuthority('admin:online-user')")
    public RestResponse<List<OnlineUserDTO>> list() {
        Map<String, String> tenantMap = tenantService.findOptions(null).stream()
                .collect(Collectors.toMap(TenantOptionDTO::getTenantId, TenantOptionDTO::getName, (a, b) -> a));
        String currentTenantId = TenantContextHolder.getTenantId();
        boolean isSuper = TenantContextHolder.isSuperTenant();

        return RestResponse.success(tokenProvider.getAllOnlineUsers(tenantMap, currentTenantId, isSuper));
    }

    @Post("/kickout")
    @Secured("hasAuthority('admin:online-user:kickout')")
    @OperLogRecord(title = "在线用户", businessType = BusinessType.FORCE)
    public RestResponse<Void> kickout(@Body Map<String, String> body) {
        String token = body.get("token");
        if (token != null) {
            tokenProvider.removeToken(token);
        }
        return RestResponse.success();
    }
}