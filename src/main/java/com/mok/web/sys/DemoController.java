package com.mok.web.sys;

import com.mok.web.common.RestResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Controller("/api/auth")
public class DemoController {

    @Get("/demo")
    public RestResponse<String> demo() {
        return RestResponse.success("ok");
    }
}
