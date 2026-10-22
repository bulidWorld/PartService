package com.partapp.partservice.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.partapp.partservice.api.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResponse<Void>> handle(ApiException e, HttpServletRequest r) {
        return ResponseEntity.status(e.httpStatus).body(new ApiResponse<>(e.code, e.getMessage(), null, id(r)));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException e, HttpServletRequest r) {
        return ResponseEntity.status(404).body(new ApiResponse<>(40401, "接口不存在", null, id(r)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleOther(Exception e, HttpServletRequest r) {
        log.error("request_id={} method={} path={} 未处理异常", id(r), r.getMethod(), r.getRequestURI(), e);
        return ResponseEntity.status(500).body(new ApiResponse<>(50000, "服务暂时不可用", null, id(r)));
    }

    private String id(HttpServletRequest r) {
        Object v = r.getAttribute("requestId");
        return v == null ? "" : v.toString();
    }
}
