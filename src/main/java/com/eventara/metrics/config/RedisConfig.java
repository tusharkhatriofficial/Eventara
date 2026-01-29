package com.eventara.metrics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for distributed metrics storage.
 * Configures RedisTemplate with appropriate serializers for metrics data.
 * 
 * Note: Spring Boot auto-configures StringRedisTemplate, so we don't define it
 * here.
 * Use @Autowired StringRedisTemplate in services that need string operations.
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate configured for metrics storage with JSON values.
     * - Keys: String (bucket keys like "metrics:bucket:1234567890")
     * - Values: JSON-serialized objects for flexibility
     */
    @Bean
    public RedisTemplate<String, Object> metricsRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys (readable)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values (flexible)
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    // StringRedisTemplate is auto-configured by Spring Boot - no need to define
    // here
}
