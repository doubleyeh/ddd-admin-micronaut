package com.mok.sys.web;

import com.mok.sys.application.dto.auth.AccountInfoDTO;
import com.mok.sys.application.dto.auth.SelfPasswordUpdateDTO;
import com.mok.sys.application.dto.user.UserDTO;
import com.mok.sys.application.dto.user.UserPasswordDTO;
import com.mok.sys.application.service.UserService;
import com.mok.common.infrastructure.tenant.TenantContextHolder;
import com.mok.common.web.RestResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountControllerTest {

    private UserService userService;
    private AccountController accountController;
    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        accountController = new AccountController(userService);
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
    }

    @Test
    void changeMyPassword() {
        String username = "testUser";
        Long userId = 1L;
        String newPassword = "newPassword123";

        SelfPasswordUpdateDTO dto = new SelfPasswordUpdateDTO();
        dto.setOldPassword("oldPassword");
        dto.setNewPassword(newPassword);

        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setUsername(username);

        tenantContextHolderMock.when(TenantContextHolder::getUsername).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(userDTO);
        doNothing().when(userService).updatePassword(any(UserPasswordDTO.class));

        RestResponse<Boolean> response = accountController.changeMyPassword(dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertTrue(response.getData());

        verify(userService).findByUsername(username);
        verify(userService).updatePassword(argThat(argument -> 
            argument.getId().equals(userId) && argument.getPassword().equals(newPassword)
        ));
    }

    @Test
    void getMyInfo() {
        String username = "testUser";
        AccountInfoDTO accountInfoDTO = new AccountInfoDTO();
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(username);
        accountInfoDTO.setUser(userDTO);

        tenantContextHolderMock.when(TenantContextHolder::getUsername).thenReturn(username);
        when(userService.findAccountInfoByUsername(username)).thenReturn(accountInfoDTO);

        RestResponse<AccountInfoDTO> response = accountController.getMyInfo();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(username, response.getData().getUser().getUsername());

        verify(userService).findAccountInfoByUsername(username);
    }

    @Test
    void updateNickname() {
        String username = "testUser";
        Long userId = 1L;
        String newNickname = "New Nickname";

        AccountController.NicknameUpdateDTO dto = new AccountController.NicknameUpdateDTO();
        dto.setNickname(newNickname);

        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setUsername(username);

        tenantContextHolderMock.when(TenantContextHolder::getUsername).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(userDTO);
        doNothing().when(userService).updateNickname(userId, newNickname);

        RestResponse<Boolean> response = accountController.updateNickname(dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertTrue(response.getData());

        verify(userService).findByUsername(username);
        verify(userService).updateNickname(userId, newNickname);
    }
}
