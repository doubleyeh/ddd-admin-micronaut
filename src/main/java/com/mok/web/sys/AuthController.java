package com.mok.web.sys;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import java.security.Principal;

@Controller("/api")
public class AuthController {

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/me")
    public String me(Principal principal) {
        return principal.getName();
    }
}