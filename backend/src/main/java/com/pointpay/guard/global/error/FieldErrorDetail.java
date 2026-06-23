package com.pointpay.guard.global.error;

public record FieldErrorDetail(
        String field,
        String message
) {
}
