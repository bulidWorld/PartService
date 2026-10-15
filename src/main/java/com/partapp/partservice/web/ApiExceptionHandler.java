package com.partapp.partservice.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.partapp.partservice.api.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResponse<Void>> handle(ApiException e, HttpServletRequest r) {
        return ResponseEntity.status(e.httpStatus).body(new ApiResponse<>(e.code, e.getMessage(), null, id(r)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleOther(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(500).body(new ApiResponse<>(50000, "服务暂时不可用", null, id(r)));
    }

    private String id(HttpServletRequest r) {
        Object v = r.getAttribute("requestId");
        return v == null ? "" : v.toString();
    }
}
