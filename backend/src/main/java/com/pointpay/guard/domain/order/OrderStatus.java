package com.pointpay.guard.domain.order;

public enum OrderStatus {
    CREATED,
    PAYING,
    PAID,
    PAYMENT_FAILED,
    CANCELED,
    SETTLED
}
