package com.grabfood.cache.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Lớp cấu hình hạ tầng Distributed Cache chuyên nghiệp sử dụng Redis & Lettuce.
 * 
 * Đáp ứng toàn diện các tiêu chí:
 * 1. Đọc thông số kết nối động từ application.yml (Không hard-code).
 * 2. Sử dụng Driver Lettuce (Non-blocking I/O) kết hợp Connection Pool.
 * 3. Chuyển đổi Serialization sang JSON (GenericJackson2JsonRedisSerializer) cho dữ liệu con người đọc được.
 * 4. Kích hoạt @EnableCaching và đăng ký Custom CacheErrorHandler (Graceful Fallback khi Redis sự cố).
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.timeout:2000ms}")
    private Duration redisTimeout;

    @Value("${spring.data.redis.lettuce.pool.max-active:10}")
    private int maxActivePool;

    @Value("${spring.data.redis.lettuce.pool.max-idle:8}")
    private int maxIdlePool;

    @Value("${spring.data.redis.lettuce.pool.min-idle:2}")
    private int minIdlePool;

    @Value("${spring.data.redis.lettuce.pool.max-wait:1000ms}")
    private Duration maxWaitPool;

    /**
     * Khởi tạo Bean RedisConnectionFactory sử dụng Lettuce Driver với cấu hình Connection Pool.
     * Lettuce là driver non-blocking dựa trên Netty, chia sẻ kết nối luồng an toàn (thread-safe).
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Khởi tạo LettuceConnectionFactory kết nối Redis tới {}:{} (Timeout: {}, MaxActivePool: {})",
                redisHost, redisPort, redisTimeout, maxActivePool);

        // 1. Cấu hình thông số Server Redis Standalone
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(redisHost);
        serverConfig.setPort(redisPort);

        // 2. Cấu hình Connection Pool cho Lettuce (Apache Commons Pool2)
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(maxActivePool);
        poolConfig.setMaxIdle(maxIdlePool);
        poolConfig.setMinIdle(minIdlePool);
        poolConfig.setMaxWait(maxWaitPool);

        // 3. Cấu hình Socket & Client Options với Timeout rõ ràng
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(redisTimeout)
                .keepAlive(true)
                .build();

        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .autoReconnect(true)
                .build();

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .clientOptions(clientOptions)
                .commandTimeout(redisTimeout)
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(serverConfig, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * Tùy biến Jackson ObjectMapper để serialize đối tượng Java sang JSON và ngược lại.
     * Hỗ trợ đầy đủ Java 8 Date/Time và lưu trữ Type Information để Deserialize chính xác.
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }

    /**
     * Khởi tạo Bean RedisCacheManager với GenericJackson2JsonRedisSerializer.
     * Dữ liệu được lưu trữ dạng JSON đọc được trực tiếp trên redis-cli.
     */
    @Bean
    @Primary
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper redisObjectMapper) {
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                // Thiết lập TTL mặc định 10 phút để dữ liệu luôn tươi mới
                .entryTtl(Duration.ofMinutes(10))
                // Không lưu giá trị null để tránh ô nhiễm bộ nhớ cache
                .disableCachingNullValues()
                // Key được serialize dưới dạng chuỗi UTF-8 dễ đọc (ví dụ: "restaurant_menus::101")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Value được serialize dưới dạng chuỗi JSON thay cho Java Binary Serializer
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .transactionAware()
                .build();
    }

    /**
     * Khởi tạo RedisTemplate cho các thao tác trực tiếp với Redis khi cần thiết.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper redisObjectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Đăng ký Custom CacheErrorHandler để đảm bảo tính sẵn sàng cao (High Availability / Fault Tolerance).
     * Khi Redis Server gặp sự cố (Down / Timeout / Network Issue), ứng dụng không bị sập (tránh HTTP 500)
     * mà tự động fallback truy vấn trực tiếp xuống Database.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.error("LỖI ĐỌC CACHE [Key: {}] trên cache [{}]: {}. Hệ thống tự động Fallback xuống Database!",
                        key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.error("LỖI GHI CACHE [Key: {}] trên cache [{}]: {}. Tiếp tục thực thi không gây gián đoạn!",
                        key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.error("LỖI XÓA CACHE [Key: {}] trên cache [{}]: {}. Cần kích hoạt cơ chế bù trừ/retry!",
                        key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.error("LỖI XÓA TOÀN BỘ CACHE [{}]: {}", cache.getName(), exception.getMessage());
            }
        };
    }
}
