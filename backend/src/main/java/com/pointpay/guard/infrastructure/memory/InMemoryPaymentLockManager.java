package com.pointpay.guard.infrastructure.memory;

import com.pointpay.guard.global.exception.ErrorCode;
import com.pointpay.guard.global.exception.LockNotAvailableException;
import com.pointpay.guard.infrastructure.redis.PointPayRedisProperties;
import com.pointpay.guard.service.redis.PaymentLockManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class InMemoryPaymentLockManager implements PaymentLockManager {

    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final PointPayRedisProperties properties;

    public InMemoryPaymentLockManager(PointPayRedisProperties properties) {
        this.properties = properties;
    }

    @Override
    public <T> T execute(String lockName, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(lockName, key -> new ReentrantLock());
        acquire(lock, lockName);
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    private void acquire(ReentrantLock lock, String lockName) {
        try {
            boolean locked = lock.tryLock(properties.getLockWaitTime().toMillis(), TimeUnit.MILLISECONDS);
            if (locked) {
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockNotAvailableException(ErrorCode.LOCK_NOT_AVAILABLE, "락 획득 대기가 중단되었습니다.");
        }

        throw new LockNotAvailableException(
                ErrorCode.LOCK_NOT_AVAILABLE,
                "동일 리소스에 대한 결제 처리가 진행 중입니다. key=" + lockName
        );
    }
}
