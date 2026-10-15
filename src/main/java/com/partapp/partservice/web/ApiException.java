package com.partapp.partservice.web;

public class ApiException extends RuntimeException {
  public final int code;
  public final int httpStatus;

  public ApiException(int code, int httpStatus, String message) {
    super(message);
    this.code = code;
    this.httpStatus = httpStatus;
  }

  public static ApiException bad(String m) {
    return new ApiException(40001, 400, m);
  }

  public static ApiException unauthorized(String m) {
    return new ApiException(40101, 401, m);
  }

  public static ApiException forbidden(String m) {
    return new ApiException(40301, 403, m);
  }

  public static ApiException notFound(String m) {
    return new ApiException(40401, 404, m);
  }

  public static ApiException conflict(String m) {
    return new ApiException(40901, 409, m);
  }
}
