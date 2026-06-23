package com.pointpay.guard.global.exception;

public enum ErrorCode {
    USER_NOT_FOUND(404),
    WALLET_NOT_FOUND(404),
    ORDER_NOT_FOUND(404),
    PAYMENT_NOT_FOUND(404),
    INVALID_PAYMENT_STATE(409),
    INVALID_ORDER_STATE(409),
    INSUFFICIENT_BALANCE(422),
    DUPLICATE_PAYMENT_REQUEST(409),
    LOCK_NOT_AVAILABLE(409),
    VALIDATION_ERROR(400);

    private final int statusCode;

    ErrorCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
