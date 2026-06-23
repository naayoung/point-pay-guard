package com.pointpay.guard.api.dto;

import com.pointpay.guard.domain.payment.PaymentEvent;
import com.pointpay.guard.domain.payment.PaymentEventType;
import com.pointpay.guard.domain.payment.PaymentStatus;
import java.time.Instant;

public record PaymentEventResponse(
        Long eventId,
        Long paymentId,
        PaymentEventType eventType,
        PaymentStatus beforeStatus,
        PaymentStatus afterStatus,
        String reason,
        Instant createdAt
) {
    public static PaymentEventResponse from(PaymentEvent event) {
        return new PaymentEventResponse(
                event.getId(),
                event.getPayment().getId(),
                event.getEventType(),
                event.getBeforeStatus(),
                event.getAfterStatus(),
                event.getReason(),
                event.getCreatedAt()
        );
    }
}
