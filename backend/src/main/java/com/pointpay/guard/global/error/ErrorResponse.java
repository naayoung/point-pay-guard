package com.pointpay.guard.global.error;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        String path,
        Instant timestamp,
        List<FieldErrorDetail> fieldErrors
) {
    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(code, message, path, Instant.now(), List.of());
    }

    public static ErrorResponse of(String code, String message, String path, List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(code, message, path, Instant.now(), fieldErrors);
    }
}
