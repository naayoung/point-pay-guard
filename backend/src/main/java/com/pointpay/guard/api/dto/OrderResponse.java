package com.pointpay.guard.api.dto;

import com.pointpay.guard.domain.order.OrderStatus;
import com.pointpay.guard.domain.order.PointOrder;
import java.time.Instant;

public record OrderResponse(
        Long orderId,
        Long userId,
        Long amount,
        OrderStatus status,
        Instant createdAt
) {
    public static OrderResponse from(PointOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getAmount(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
