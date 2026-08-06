package com.ladiesapparel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    /** Plain string template — used by the Redis-backed rate limiter (INCR/EXPIRE), not the cache abstraction. */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl(Duration.ofMinutes(15));

        Map<String, RedisCacheConfiguration> perCacheTtls = new HashMap<>();
        perCacheTtls.put("categoryTree", defaultConfig.entryTtl(Duration.ofHours(1)));
        perCacheTtls.put("categoriesFlat", defaultConfig.entryTtl(Duration.ofHours(1)));
        perCacheTtls.put("productDetail", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        perCacheTtls.put("activeBanners", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        perCacheTtls.put("dashboardSummary", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        perCacheTtls.put("topProducts", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        perCacheTtls.put("lowStock", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return builder -> builder.cacheDefaults(defaultConfig).withInitialCacheConfigurations(perCacheTtls);
    }
}
