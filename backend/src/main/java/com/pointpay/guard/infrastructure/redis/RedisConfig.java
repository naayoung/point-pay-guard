package com.pointpay.guard.infrastructure.redis;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PointPayRedisProperties.class)
public class RedisConfig {
}
