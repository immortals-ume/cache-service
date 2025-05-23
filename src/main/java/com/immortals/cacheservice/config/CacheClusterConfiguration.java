package com.immortals.cacheservice.config;

import com.immortals.cacheservice.service.RedisCacheService;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@Profile("cluster")
@RequiredArgsConstructor
@Slf4j
public class CacheClusterConfiguration {

    @Bean
    public CacheProperties cacheProperties() {
        return new CacheProperties();
    }

    @Bean(destroyMethod = "destroy")
    public LettuceConnectionFactory redisClusterConnectionFactory(final CacheProperties props) {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(props.getCluster().getNodes());
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(props.getCommandTimeout())
                .shutdownTimeout(Duration.ZERO)
                .readFrom(ReadFrom.REPLICA_PREFERRED)
                .useSsl()
                .and()
                .clientOptions(ClientOptions.builder()
                        .autoReconnect(props.getAutoReconnect())
                        .pingBeforeActivateConnection(props.getPingBeforeActivateConnection())
                        .build())
                .build();

        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(@Qualifier("redisClusterConnectionFactory") RedisConnectionFactory factory) {
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
