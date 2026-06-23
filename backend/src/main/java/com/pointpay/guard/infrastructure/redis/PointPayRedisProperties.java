package com.pointpay.guard.infrastructure.redis;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pointpay.redis")
public class PointPayRedisProperties {

    private Duration idempotencyTtl = Duration.ofMinutes(10);
    private Duration lockWaitTime = Duration.ofMillis(500);
    private Duration lockLeaseTime = Duration.ofSeconds(5);

    public Duration getIdempotencyTtl() {
        return idempotencyTtl;
    }

    public void setIdempotencyTtl(Duration idempotencyTtl) {
        this.idempotencyTtl = idempotencyTtl;
    }

    public Duration getLockWaitTime() {
        return lockWaitTime;
    }

    public void setLockWaitTime(Duration lockWaitTime) {
        this.lockWaitTime = lockWaitTime;
    }

    public Duration getLockLeaseTime() {
        return lockLeaseTime;
    }

    public void setLockLeaseTime(Duration lockLeaseTime) {
        this.lockLeaseTime = lockLeaseTime;
    }
}
