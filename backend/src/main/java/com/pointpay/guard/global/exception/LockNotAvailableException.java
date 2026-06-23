package com.pointpay.guard.global.exception;

public class LockNotAvailableException extends BusinessException {

    public LockNotAvailableException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
