package com.pointpay.guard.domain.payment;

public enum PaymentEventType {
    PAYMENT_CREATED,
    PAYMENT_APPROVING,
    PAYMENT_APPROVED,
    PAYMENT_FAILED,
    PAYMENT_CANCELING,
    PAYMENT_CANCELED,
    PAYMENT_SETTLED;

    public static PaymentEventType fromAfterStatus(PaymentStatus status) {
        return switch (status) {
            case READY -> PAYMENT_CREATED;
            case APPROVING -> PAYMENT_APPROVING;
            case APPROVED -> PAYMENT_APPROVED;
            case FAILED -> PAYMENT_FAILED;
            case CANCELING -> PAYMENT_CANCELING;
            case CANCELED -> PAYMENT_CANCELED;
            case SETTLED -> PAYMENT_SETTLED;
        };
    }
}
