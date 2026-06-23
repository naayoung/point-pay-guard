package com.pointpay.guard.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
        @NotNull Long userId,
        @NotNull @Positive Long amount
) {
}
