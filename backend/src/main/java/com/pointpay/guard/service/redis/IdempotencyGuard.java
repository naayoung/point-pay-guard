package com.pointpay.guard.service.redis;

public interface IdempotencyGuard {

    boolean claim(String idempotencyKey);

    void complete(String idempotencyKey, Long paymentId);

    void release(String idempotencyKey);
}
