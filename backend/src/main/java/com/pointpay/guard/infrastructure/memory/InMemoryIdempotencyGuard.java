package com.pointpay.guard.infrastructure.memory;

import com.pointpay.guard.service.redis.IdempotencyGuard;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class InMemoryIdempotencyGuard implements IdempotencyGuard {

    private static final String PROCESSING = "PROCESSING";

    private final ConcurrentMap<String, String> idempotencyKeys = new ConcurrentHashMap<>();

    @Override
    public boolean claim(String idempotencyKey) {
        return idempotencyKeys.putIfAbsent(idempotencyKey, PROCESSING) == null;
    }

    @Override
    public void complete(String idempotencyKey, Long paymentId) {
        idempotencyKeys.put(idempotencyKey, "PAYMENT:" + paymentId);
    }

    @Override
    public void release(String idempotencyKey) {
        idempotencyKeys.remove(idempotencyKey);
    }
}
