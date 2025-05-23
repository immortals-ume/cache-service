package com.immortals.cacheservice.config;

import com.immortals.cacheservice.service.RedisCacheService;
import io.lettuce.core.ClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

import static io.lettuce.core.ReadFrom.REPLICA_PREFERRED;

@Configuration
@Slf4j
public class CacheConfiguration {

    @Bean
    public CacheProperties cacheProperties() {
        return new CacheProperties();
    }

    @Bean(destroyMethod = "destroy")
    public LettuceConnectionFactory lettuceConnectionFactory(CacheProperties props) {
        log.info("Initializing LettuceConnectionFactory with host: {}, port: {}", props.getHost(), props.getPort());

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(props.getHost());
        redisConfig.setPort(props.getPort());
        if (props.getPassword() != null && !props.getPassword().isBlank()) {
            redisConfig.setPassword(props.getPassword());
        }

        redisConfig.setDatabase(props.getDatabase());


        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(props.getCommandTimeout())
                .useSsl()
                .and()
                .shutdownTimeout(Duration.ZERO)
                .readFrom(REPLICA_PREFERRED)
                .clientOptions(ClientOptions.builder()
                        .autoReconnect(props.getAutoReconnect())
                        .pingBeforeActivateConnection(props.getAutoReconnect())
                        .build())
                .build();


        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(@Qualifier("lettuceConnectionFactory") RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();

        log.info("RedisTemplate initialized with custom Jackson2JsonRedisSerializer");
        return template;
    }

    @Bean
    public RedisCacheService<String, Object> cacheServiceImplementation(
            RedisTemplate<String, Object> redisTemplate,
            CacheProperties cacheProperties) {
        log.info("Initializing RedisCacheService with TTL: {}", cacheProperties.getTimeToLive());
        return new RedisCacheService<>(redisTemplate, cacheProperties.getTimeToLive());
    }

}
