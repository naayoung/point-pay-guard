package com.pointpay.guard.infrastructure.redis;

import com.pointpay.guard.global.exception.ErrorCode;
import com.pointpay.guard.global.exception.LockNotAvailableException;
import com.pointpay.guard.service.redis.PaymentLockManager;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@Profile("!demo")
public class RedisPaymentLockManager implements PaymentLockManager {

    private static final String LOCK_PREFIX = "lock:";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            // 내가 잡은 락 토큰과 일치할 때만 삭제해서 다른 요청의 락을 실수로 해제하지 않는다.
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final PointPayRedisProperties properties;

    public RedisPaymentLockManager(StringRedisTemplate redisTemplate, PointPayRedisProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public <T> T execute(String lockName, Supplier<T> action) {
        String key = LOCK_PREFIX + lockName;
        String token = UUID.randomUUID().toString();
        acquire(key, token);
        try {
            return action.get();
        } finally {
            release(key, token);
        }
    }

    private void acquire(String key, String token) {
        Instant deadline = Instant.now().plus(properties.getLockWaitTime());
        do {
            // lease time을 둬서 프로세스가 죽어도 락이 영구히 남지 않게 한다.
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(key, token, properties.getLockLeaseTime());
            if (Boolean.TRUE.equals(locked)) {
                return;
            }
            sleepBriefly();
        } while (Instant.now().isBefore(deadline));

        throw new LockNotAvailableException(
                ErrorCode.LOCK_NOT_AVAILABLE,
                "동일 리소스에 대한 결제 처리가 진행 중입니다. key=" + key
        );
    }

    private void release(String key, String token) {
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(50L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockNotAvailableException(ErrorCode.LOCK_NOT_AVAILABLE, "락 획득 대기가 중단되었습니다.");
        }
    }
}
