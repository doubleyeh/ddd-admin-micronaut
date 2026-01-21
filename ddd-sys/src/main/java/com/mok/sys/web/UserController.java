package com.mok.sys.web;

import com.mok.sys.application.dto.user.*;
import com.mok.sys.application.service.UserService;
import com.mok.common.infrastructure.common.Const;
import com.mok.sys.infrastructure.log.BusinessType;
import com.mok.sys.infrastructure.log.OperLogRecord;
import com.mok.common.infrastructure.util.PasswordGenerator;
import com.mok.common.web.RestResponse;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.utils.SecurityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.Optional;

@Controller("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SecurityService securityService;

    @Get
    @Secured("hasAuthority('user:list')")
    public RestResponse<Page<@NonNull UserDTO>> findPage(UserQuery query, Pageable pageable) {
        Page<@NonNull UserDTO> page = userService.findPage(query, pageable);
        return RestResponse.success(page);
    }

    @Get("/{id}")
    @Secured("hasAuthority('user:list')")
    public RestResponse<UserDTO> getById(@PathVariable Long id) {
        UserDTO userDTO = userService.getById(id);
        if (Objects.isNull(userDTO)) {
            return RestResponse.failure(404, Const.NOT_FOUND_MESSAGE);
        }
        return RestResponse.success(userDTO);
    }

    @Post
    @Secured("hasAuthority('user:create')")
    @OperLogRecord(title = "用户管理", businessType = BusinessType.INSERT)
    public RestResponse<UserDTO> save(@Body @Valid UserPostDTO userDTO) {
        UserDTO savedUser = userService.create(userDTO);
        return RestResponse.success(savedUser);
    }

    @Put("/{id}")
    @Secured("hasAuthority('user:update')")
    @OperLogRecord(title = "用户管理", businessType = BusinessType.UPDATE)
    public RestResponse<UserDTO> update(@PathVariable Long id, @Body @Valid UserPutDTO userDTO) {
        userDTO.setId(id);
        UserDTO updatedUser = userService.updateUser(userDTO);
        return RestResponse.success(updatedUser);
    }

    @Put("/{id}/state")
    @Secured("hasAuthority('user:update')")
    @OperLogRecord(title = "用户管理", businessType = BusinessType.UPDATE)
    public RestResponse<UserDTO> updateState(@PathVariable Long id, @Valid @NotNull @QueryValue Integer state) {
        if (Objects.equals(0, state) && isCurrentUser(id)) {
            return RestResponse.failure("禁止禁用当前登录用户");
        }
        UserDTO dto = userService.updateUserState(id, state);
        return RestResponse.success(dto);
    }

    @Put("/{id}/password")
    @Secured("hasAuthority('user:update')")
    @OperLogRecord(title = "用户管理", businessType = BusinessType.UPDATE)
    public RestResponse<String> resetPassword(@PathVariable Long id) {
        String newPassword = PasswordGenerator.generateRandomPassword();

        UserPasswordDTO passwordDTO = new UserPasswordDTO();
        passwordDTO.setId(id);
        passwordDTO.setPassword(newPassword);

        userService.updatePassword(passwordDTO);
        return RestResponse.success(newPassword);
    }

    @Delete("/{id}")
    @Secured("hasAuthority('user:delete')")
    @OperLogRecord(title = "用户管理", businessType = BusinessType.DELETE)
    public RestResponse<Void> deleteById(@PathVariable Long id) {
        if (isCurrentUser(id)) {
            return RestResponse.failure("禁止删除当前登录用户");
        }
        userService.deleteById(id);
        return RestResponse.success();
    }

    private boolean isCurrentUser(Long id) {
        Optional<Long> currentUserIdOpt = securityService.getAuthentication()
                .map(auth -> auth.getAttributes().get("userId"))
                .map(Long.class::cast);
        return currentUserIdOpt.isPresent() && currentUserIdOpt.get().equals(id);
    }
}
