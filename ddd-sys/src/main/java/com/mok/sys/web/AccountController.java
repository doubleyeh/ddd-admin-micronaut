package com.mok.sys.web;

import com.mok.sys.application.dto.auth.AccountInfoDTO;
import com.mok.sys.application.dto.auth.SelfPasswordUpdateDTO;
import com.mok.sys.application.dto.user.UserDTO;
import com.mok.sys.application.dto.user.UserPasswordDTO;
import com.mok.sys.application.service.UserService;
import com.mok.sys.infrastructure.log.BusinessType;
import com.mok.sys.infrastructure.log.OperLogRecord;
import com.mok.common.infrastructure.tenant.TenantContextHolder;
import com.mok.common.web.RestResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Controller("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;

    @Put("/password")
    @OperLogRecord(title = "个人信息", businessType = BusinessType.UPDATE)
    public RestResponse<Boolean> changeMyPassword(Authentication authentication, @Body @Valid SelfPasswordUpdateDTO dto) {
        UserDTO user = userService.findByUsername(authentication.getName());

        UserPasswordDTO passwordDTO = new UserPasswordDTO();
        passwordDTO.setId(user.getId());
        passwordDTO.setPassword(dto.getNewPassword());
        userService.updatePassword(passwordDTO);
        return RestResponse.success(true);
    }

    @Get("/info")
    public RestResponse<AccountInfoDTO> getMyInfo(Authentication authentication) {
        AccountInfoDTO user = userService.findAccountInfoByUsername(authentication.getName());
        return RestResponse.success(user);
    }

    @Put("/nickname")
    public RestResponse<Boolean> updateNickname(Authentication authentication, @Body @Valid NicknameUpdateDTO dto) {
        String username = authentication.getName();
        UserDTO user = userService.findByUsername(username);
        userService.updateNickname(user.getId(), dto.getNickname());
        return RestResponse.success(true);
    }

    @Data
    @Serdeable
    public static class NicknameUpdateDTO implements Serializable {

        @NotBlank(message = "昵称不允许为空")
        @Size(max = 50, message = "昵称长度不能超过50个字符")
        private String nickname;
    }
}
