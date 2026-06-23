package com.pointpay.guard.infrastructure.redis;

import com.pointpay.guard.service.redis.IdempotencyGuard;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!demo")
public class RedisIdempotencyGuard implements IdempotencyGuard {

    private static final String KEY_PREFIX = "payment:idempotency:";
    private static final String PROCESSING = "PROCESSING";

    private final StringRedisTemplate redisTemplate;
    private final PointPayRedisProperties properties;

    public RedisIdempotencyGuard(StringRedisTemplate redisTemplate, PointPayRedisProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public boolean claim(String idempotencyKey) {
        // SETNX 성격의 setIfAbsent로 최초 요청만 PROCESSING 상태를 선점한다.
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(key(idempotencyKey), PROCESSING, properties.getIdempotencyTtl()));
    }

    @Override
    public void complete(String idempotencyKey, Long paymentId) {
        // 완료 후에는 같은 키로 재요청이 와도 기존 결제 결과를 조회할 수 있게 결제 id를 남긴다.
        redisTemplate.opsForValue()
                .set(key(idempotencyKey), "PAYMENT:" + paymentId, properties.getIdempotencyTtl());
    }

    @Override
    public void release(String idempotencyKey) {
        redisTemplate.delete(key(idempotencyKey));
    }

    private String key(String idempotencyKey) {
        return KEY_PREFIX + idempotencyKey;
    }
}
