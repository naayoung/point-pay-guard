package com.pointpay.guard.global.exception;

public class DuplicatePaymentRequestException extends BusinessException {

    public DuplicatePaymentRequestException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
