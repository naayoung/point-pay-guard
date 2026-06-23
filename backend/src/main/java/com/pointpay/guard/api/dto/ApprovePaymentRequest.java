package com.pointpay.guard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApprovePaymentRequest(
        @NotNull Long orderId,
        @NotBlank String idempotencyKey
) {
    public ApprovePaymentRequest {
        if (idempotencyKey != null) {
            idempotencyKey = idempotencyKey.trim();
        }
    }
}
