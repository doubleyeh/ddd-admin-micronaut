package com.mok.web.sys;

import com.mok.application.sys.dto.user.*;
import com.mok.application.sys.service.UserService;
import com.mok.infrastructure.tenant.TenantContextHolder;
import com.mok.web.common.RestResponse;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private UserService userService;
    private UserController userController;
    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userController = new UserController(userService);
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
    }

    @Test
    void findPage() {
        UserQuery query = new UserQuery();
        Pageable pageable = Pageable.from(0, 10);
        Page<UserDTO> page = Page.of(Collections.emptyList(), pageable, 0L);

        when(userService.findPage(query, pageable)).thenReturn(page);

        RestResponse<Page<UserDTO>> response = userController.findPage(query, pageable);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(page, response.getData());

        verify(userService).findPage(query, pageable);
    }

    @Test
    void getById() {
        Long id = 1L;
        UserDTO userDTO = new UserDTO();
        userDTO.setId(id);

        when(userService.getById(id)).thenReturn(userDTO);

        RestResponse<UserDTO> response = userController.getById(id);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(userDTO, response.getData());

        verify(userService).getById(id);
    }

    @Test
    void getById_NotFound() {
        Long id = 1L;
        when(userService.getById(id)).thenReturn(null);

        RestResponse<UserDTO> response = userController.getById(id);

        assertNotNull(response);
        assertEquals(404, response.getCode());
        assertFalse(response.isState());

        verify(userService).getById(id);
    }

    @Test
    void save() {
        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("newUser");
        UserDTO createdDto = new UserDTO();
        createdDto.setId(1L);
        createdDto.setUsername("newUser");

        when(userService.create(dto)).thenReturn(createdDto);

        RestResponse<UserDTO> response = userController.save(dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(createdDto, response.getData());

        verify(userService).create(dto);
    }

    @Test
    void update() {
        Long id = 1L;
        UserPutDTO dto = new UserPutDTO();
        dto.setUsername("updatedUser");
        UserDTO updatedDto = new UserDTO();
        updatedDto.setId(id);
        updatedDto.setUsername("updatedUser");

        when(userService.updateUser(dto)).thenReturn(updatedDto);

        RestResponse<UserDTO> response = userController.update(id, dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(updatedDto, response.getData());
        assertEquals(id, dto.getId());

        verify(userService).updateUser(dto);
    }

    @Test
    void updateState() {
        Long id = 1L;
        Integer state = 1;
        UserDTO updatedDto = new UserDTO();
        updatedDto.setId(id);
        updatedDto.setState(state);

        tenantContextHolderMock.when(TenantContextHolder::getUserId).thenReturn(2L);
        when(userService.updateUserState(id, state)).thenReturn(updatedDto);

        RestResponse<UserDTO> response = userController.updateState(id, state);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(updatedDto, response.getData());

        verify(userService).updateUserState(id, state);
    }

    @Test
    void updateState_CurrentUser_Forbidden() {
        Long id = 1L;
        Integer state = 0;

        tenantContextHolderMock.when(TenantContextHolder::getUserId).thenReturn(id);

        RestResponse<UserDTO> response = userController.updateState(id, state);

        assertNotNull(response);
        assertEquals(400, response.getCode());
        assertFalse(response.isState());
        assertEquals("禁止禁用当前登录用户", response.getMessage());

        verify(userService, never()).updateUserState(anyLong(), anyInt());
    }

    @Test
    void resetPassword() {
        Long id = 1L;
        doNothing().when(userService).updatePassword(any(UserPasswordDTO.class));

        RestResponse<String> response = userController.resetPassword(id);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertNotNull(response.getData());

        verify(userService).updatePassword(any(UserPasswordDTO.class));
    }

    @Test
    void deleteById() {
        Long id = 1L;
        tenantContextHolderMock.when(TenantContextHolder::getUserId).thenReturn(2L);
        doNothing().when(userService).deleteById(id);

        RestResponse<Void> response = userController.deleteById(id);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(userService).deleteById(id);
    }

    @Test
    void deleteById_CurrentUser_Forbidden() {
        Long id = 1L;
        tenantContextHolderMock.when(TenantContextHolder::getUserId).thenReturn(id);

        RestResponse<Void> response = userController.deleteById(id);

        assertNotNull(response);
        assertEquals(400, response.getCode());
        assertFalse(response.isState());
        assertEquals("禁止删除当前登录用户", response.getMessage());

        verify(userService, never()).deleteById(anyLong());
    }

    @Test
    void updateState_GetUserIdNull() {
        Long id = 1L;
        Integer state = 0;
        UserDTO updatedDto = new UserDTO();
        updatedDto.setId(id);
        updatedDto.setState(state);

        tenantContextHolderMock.when(TenantContextHolder::getUserId).thenReturn(null);
        when(userService.updateUserState(id, state)).thenReturn(updatedDto);

        RestResponse<UserDTO> response = userController.updateState(id, state);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(updatedDto, response.getData());

        verify(userService).updateUserState(id, state);
    }

    @Test
    void deleteById_GetUserIdNull() {
        Long id = 1L;
        tenantContextHolderMock.when(TenantContextHolder::getUserId).thenReturn(null);
        doNothing().when(userService).deleteById(id);

        RestResponse<Void> response = userController.deleteById(id);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(userService).deleteById(id);
    }
}
