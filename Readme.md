# Redis Cache Service Module

A production-ready, pluggable Redis-based caching module for Spring Boot applications. Built with robust thread safety,
transactional guarantees, observability, and flexible Redis configuration (standalone, sentinel, or cluster-ready).

## Features

* **Generic Cache Interface**: Define your own cache with `CacheService<K, V>`.
* **Redis Implementation**: `RedisCacheService<K, V>` with TTL support.
* **Thread Safety**: Atomic operations and Java locks for metrics.
* **Transactional Support**: Spring `@Transactional` annotations for consistency.
* **Metrics**: Hit/Miss counters with reset capability.
* **Serialization**: JSON serialization using `GenericJackson2JsonRedisSerializer`.
* **Connection Pooling**: Lettuce pooling configuration.
* **SSL Support**: Optional TLS for secure communication.
* **Auto-Configuration**: Spring Boot auto-configuration via `spring.factories`.
* **Graceful Shutdown**: Logs metrics on shutdown.

## Installation

Add the module to your project as a Maven dependency:

```xml

<dependency>
    <groupId>com.immortals</groupId>
    <artifactId>cache-service-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Configuration

Define Redis and cache settings in your `application.yml`:

```yaml
immortals:
  cache:
    host: redis-host.example.com
    port: 6379
    password: ${REDIS_PASSWORD:}
    database: 0
    command-timeout: 2s
    time-to-live: 300s
    use-ssl: false

    pool-max-total: 16
    pool-max-idle: 16
    pool-min-idle: 4
    pool-max-wait: 1s
    auto-reconnect: true
```

> **Note**: Ensure sensitive values like `password` are managed via environment variables or a secrets manager.

## Usage

Inject and use `CacheService` in your Spring components:

```java
import com.immortals.cacheservice.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final CacheService<String, Object> cacheService;

    @Autowired
    public UserService(CacheService<String, Object> cacheService) {
        this.cacheService = cacheService;
    }

    public User getUser(String userId) {
        // Try cache first
        User user = (User) cacheService.get("user:" + userId);
        if (user == null) {
            // Load from DB
            user = loadUserFromDatabase(userId);
            // Put in cache
            cacheService.putIfAbsent("user:" + userId, user);
        }
        return user;
    }
}
```

## Metrics & Management

* **Hit Count**: `cacheService.getHitCount()`
* **Miss Count**: `cacheService.getMissCount()`
* **Reset Metrics**: `cacheService.resetMetrics()`

These can also be exposed via Spring Actuator by registering gauges in your application config.

## Auto-Configuration

The module auto-configures via `META-INF/spring.factories`:

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.immortals.cacheservice.config.CacheConfiguration
```

## Extending for Sentinel / Cluster

Support for Redis Sentinel and Cluster is coming soon. The configuration structure will follow the same
`CacheProperties` binding pattern with additional properties under `immortals.cache`.

## Testing

Use Testcontainers for integration testing:

```java
// Example JUnit 5
@Container
static GenericContainer<?> redis = new GenericContainer<>("redis:6.2").withExposedPorts(6379);

@BeforeAll
static void setup() {
    System.setProperty("immortals.cache.host", redis.getContainerIpAddress());
    System.setProperty("immortals.cache.port", redis.getMappedPort(6379).toString());
}
```

## License

This project is licensed under the MIT License. See `LICENSE` for details.
