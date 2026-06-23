package com.pointpay.guard.global.exception;

public class InvalidPaymentStateException extends BusinessException {

    public InvalidPaymentStateException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
