package com.mok.common.web;

import com.mok.common.application.exception.BizException;
import com.mok.common.application.exception.NotFoundException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.authentication.AuthorizationException;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Produces
@Singleton
public class GlobalExceptionHandler implements ExceptionHandler<Throwable, HttpResponse<RestResponse<?>>> {

    @Override
    public HttpResponse<RestResponse<?>> handle(HttpRequest request, Throwable e) {
        return switch (e) {
            case BizException ex -> HttpResponse.ok(RestResponse.failure(500, ex.getMessage()));
            case NotFoundException ex -> HttpResponse.ok(RestResponse.failure(404, ex.getMessage()));
            case ConstraintViolationException ex -> handleValidation(ex);
            case AuthenticationException ex -> HttpResponse.status(HttpStatus.UNAUTHORIZED).body(RestResponse.failure(401, ex.getMessage()));
            case AuthorizationException ex -> HttpResponse.status(HttpStatus.FORBIDDEN).body(RestResponse.failure(403, "权限不足"));
            case Exception ex -> handleGeneric(ex);
            default -> HttpResponse.ok(RestResponse.failure(500, "未知错误"));
        };
    }

    private HttpResponse<RestResponse<?>> handleValidation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("参数校验失败");
        return HttpResponse.ok(RestResponse.failure(400, message));
    }

    private HttpResponse<RestResponse<?>> handleGeneric(Exception e) {
        log.error("Unhandled Exception: ", e);
        return HttpResponse.ok(RestResponse.failure(500, "系统繁忙，请稍后重试"));
    }
}
