package com.mok.web.sys;

import com.mok.application.sys.dto.log.LoginLogDTO;
import com.mok.application.sys.dto.log.LoginLogQuery;
import com.mok.application.sys.service.LoginLogService;
import com.mok.web.common.RestResponse;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import lombok.RequiredArgsConstructor;


@Controller("/api/login-logs")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @Get
    @Secured("hasAuthority('log:login:list')")
    public RestResponse<Page<LoginLogDTO>> findPage(LoginLogQuery query, Pageable pageable) {
        Page<LoginLogDTO> page = loginLogService.findPage(query, pageable);
        return RestResponse.success(page);
    }
}
