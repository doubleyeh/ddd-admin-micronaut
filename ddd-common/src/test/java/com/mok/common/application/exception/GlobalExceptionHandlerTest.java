package com.mok.common.application.exception;

import com.mok.common.application.exception.BizException;
import com.mok.common.application.exception.NotFoundException;
import com.mok.common.web.GlobalExceptionHandler;
import com.mok.common.web.RestResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.authentication.AuthorizationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private HttpRequest<?> request;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpRequest.class);
    }

    @Test
    void handleBizException() {
        BizException exception = new BizException("业务异常");
        HttpResponse<RestResponse<?>> response = globalExceptionHandler.handle(request, exception);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(500, response.body().getCode());
        assertEquals("业务异常", response.body().getMessage());
        assertFalse(response.body().isState());
    }

    @Test
    void handleNotFoundException() {
        NotFoundException exception = new NotFoundException("未找到资源");
        HttpResponse<RestResponse<?>> response = globalExceptionHandler.handle(request, exception);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(404, response.body().getCode());
        assertEquals("未找到资源", response.body().getMessage());
        assertFalse(response.body().isState());
    }

    @Test
    void handleConstraintViolationException() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("参数错误");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        HttpResponse<RestResponse<?>> response = globalExceptionHandler.handle(request, exception);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(400, response.body().getCode());
        assertEquals("参数错误", response.body().getMessage());
        assertFalse(response.body().isState());
    }

    @Test
    void handleAuthenticationException() {
        AuthenticationException exception = new AuthenticationException("认证失败");
        HttpResponse<RestResponse<?>> response = globalExceptionHandler.handle(request, exception);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
        assertEquals(401, response.body().getCode());
        assertEquals("认证失败", response.body().getMessage());
        assertFalse(response.body().isState());
    }

    @Test
    void handleAuthorizationException() {
        AuthorizationException exception = new AuthorizationException(null);
        HttpResponse<RestResponse<?>> response = globalExceptionHandler.handle(request, exception);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatus());
        assertEquals(403, response.body().getCode());
        assertEquals("权限不足", response.body().getMessage());
        assertFalse(response.body().isState());
    }

    @Test
    void handleGenericException() {
        Exception exception = new Exception("未知错误");
        HttpResponse<RestResponse<?>> response = globalExceptionHandler.handle(request, exception);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(500, response.body().getCode());
        assertEquals("系统繁忙，请稍后重试", response.body().getMessage());
        assertFalse(response.body().isState());
    }
}
