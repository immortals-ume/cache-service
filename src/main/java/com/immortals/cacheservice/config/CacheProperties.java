package com.immortals.cacheservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "cache.redis")
public class CacheProperties {

    private String host;
    private Integer port;
    private String password;
    private Integer database;
    private Duration commandTimeout;
    private Duration timeToLive;
    private Boolean useSsl;
    private Boolean autoReconnect;
    private Integer poolMaxTotal;
    private Integer poolMaxIdle;
    private Integer poolMinIdle;
    private Duration poolMaxWait;
}
