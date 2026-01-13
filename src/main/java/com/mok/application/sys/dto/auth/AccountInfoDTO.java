package com.mok.application.sys.dto.auth;

import com.mok.application.sys.dto.menu.MenuDTO;
import com.mok.application.sys.dto.user.UserDTO;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collection;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Serdeable
public class AccountInfoDTO implements Serializable {
    UserDTO user;

    Collection<MenuDTO> menus;

    Collection<String> permissions;
}