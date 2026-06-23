package com.pointpay.guard.api.dto;

import com.pointpay.guard.domain.payment.Payment;
import com.pointpay.guard.domain.payment.PaymentStatus;
import java.time.Instant;

public record PaymentResponse(
        Long paymentId,
        Long orderId,
        Long userId,
        Long amount,
        PaymentStatus status,
        String idempotencyKey,
        Instant approvedAt,
        Instant canceledAt,
        Instant settledAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getUser().getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getIdempotencyKey(),
                payment.getApprovedAt(),
                payment.getCanceledAt(),
                payment.getSettledAt()
        );
    }
}
