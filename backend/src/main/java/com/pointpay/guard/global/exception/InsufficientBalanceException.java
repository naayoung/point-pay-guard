package com.pointpay.guard.global.exception;

public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
