package com.mok.infrastructure.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mok.domain.sys.model.OperLog;
import com.mok.infrastructure.tenant.TenantContextHolder;
import com.mok.infrastructure.util.SysUtil;
import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.context.ServerRequestContext;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@InterceptorBean(OperLogRecord.class)
@RequiredArgsConstructor
public class OperLogInterceptor implements MethodInterceptor<Object, Object> {

    private final ApplicationEventPublisher<OperLogEvent> eventPublisher;
    private final ObjectMapper objectMapper;

    @Nullable
    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;

        try {
            result = context.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            Exception finalException = exception;
            Object finalResult = result;
            if (context != null) {
                context.findAnnotation(OperLogRecord.class).ifPresent(av -> {
                    handleLog(context, av, finalException, finalResult, costTime);
                });
            }
        }
    }

    protected void handleLog(final MethodInvocationContext<Object, Object> context,
                             AnnotationValue<OperLogRecord> operLogAnnotation, final Exception e, Object jsonResult, long costTime) {
        try {
            int status = 1;
            String errorMsg = null;
            if (e != null) {
                status = 0;
                errorMsg = e.getMessage();
            }

            HttpRequest<?> request = ServerRequestContext.currentRequest().orElse(null);
            String operUrl = "";
            String requestMethod = "";
            String ip = "127.0.0.1";
            if (request != null) {
                ip = SysUtil.getIpAddress(request);
                operUrl = request.getPath();
                requestMethod = request.getMethodName();
            }

            String username = TenantContextHolder.getUsername();
            String tenantId = TenantContextHolder.getTenantId();

            String className = context.getTarget().getClass().getName();
            String methodName = context.getExecutableMethod().getMethodName();
            String method = className + "." + methodName + "()";

            String title = operLogAnnotation.get("title", String.class).orElse("");
            Integer businessType = operLogAnnotation.enumValue("businessType", BusinessType.class).orElse(BusinessType.OTHER).ordinal();

            String operParam = null;
            boolean isSaveRequestData = operLogAnnotation.isTrue("isSaveRequestData");
            if (isSaveRequestData) {
                operParam = getRequestValue(context);
            }

            String resultJson = null;
            if (isSaveRequestData && jsonResult != null) {
                try {
                    resultJson = objectMapper.writeValueAsString(jsonResult);
                } catch (Exception ex) {
                    resultJson = "error serializing result";
                }
            }

            OperLog operLog = OperLog.create(title, businessType, method, requestMethod, username, operUrl, ip, operParam, resultJson, status, errorMsg, costTime);
            operLog.assignTenant(tenantId);
            operLog.assignCreator(username);

            eventPublisher.publishEvent(new OperLogEvent(operLog));
        } catch (Exception exp) {
            log.error("==OperLogInterceptor异常==");
            log.error("异常信息:{}", exp.getMessage(), exp);
        }
    }

    private String getRequestValue(MethodInvocationContext<Object, Object> context) {
        Object[] args = context.getParameterValues();
        if (args == null || args.length == 0) {
            return null;
        }

        try {
            String params = objectMapper.writeValueAsString(args);
            if (params.length() > 2000) {
                return params.substring(0, 2000) + "...";
            }
            return params;
        } catch (Exception e) {
            return "error serializing args";
        }
    }
}
