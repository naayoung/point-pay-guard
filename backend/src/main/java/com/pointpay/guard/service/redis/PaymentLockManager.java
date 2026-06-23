package com.pointpay.guard.service.redis;

import java.util.function.Supplier;

public interface PaymentLockManager {

    <T> T execute(String lockName, Supplier<T> action);
}
