package com.immortals.cacheservice.service;

import com.immortals.cacheservice.service.exception.CacheException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class RedisCacheService<K, V> implements CacheService<K, V> {

    private final RedisTemplate<K, V> redisTemplate;
    private final ValueOperations<K, V> valueOps;
    private final Duration ttl;

    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    private final ReentrantLock metricsLock = new ReentrantLock();

    public RedisCacheService(RedisTemplate<K, V> redisTemplate, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
        this.ttl = ttl;
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = {CacheException.class, RuntimeException.class}
    )
    public void put(K key, V value) {
        try {
            valueOps.set(key, value, ttl);
        } catch (DataAccessException e) {
            log.error("Redis PUT failed for key [{}]: {}", key, e.getMessage(), e);
            throw new CacheException("Failed to put value in cache", e);
        }
    }

    /**
     * Thread-safe atomic put-if-absent operation using Redis SETNX.
     * Returns true if key was absent and value was set, false if key already exists.
     */
    public boolean putIfAbsent(K key, V value) {
        try {
            Boolean success = valueOps.setIfAbsent(key, value, ttl);
            if (Boolean.TRUE.equals(success)) {
                hits.incrementAndGet();
                return Boolean.TRUE;
            } else {
                misses.incrementAndGet();
                return Boolean.FALSE;
            }
        } catch (DataAccessException e) {
            log.error("Redis putIfAbsent failed for key [{}]: {}", key, e.getMessage(), e);
            misses.incrementAndGet();
            throw new CacheException("Failed to perform putIfAbsent in cache", e);
        }
    }

    @Override
    public V get(K key) {
        try {
            V value = valueOps.get(key);
            if (value != null) {
                hits.incrementAndGet();
            } else {
                misses.incrementAndGet();
            }
            return value;
        } catch (DataAccessException e) {
            misses.incrementAndGet();
            log.error("Redis GET failed for key [{}]: {}", key, e.getMessage(), e);
            throw new CacheException("Failed to get value from cache", e);
        }
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = {CacheException.class, RuntimeException.class}
    )
    public void remove(K key) {
        try {
            redisTemplate.delete(key);
        } catch (DataAccessException e) {
            log.error("Redis DELETE failed for key [{}]: {}", key, e.getMessage(), e);
            throw new CacheException("Failed to remove cache key", e);
        }
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = {CacheException.class, RuntimeException.class}
    )
    public void clear() {
        try {
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                connection.serverCommands();
                return null;
            });
        } catch (DataAccessException e) {
            log.error("Redis FLUSH DB failed: {}", e.getMessage(), e);
            throw new CacheException("Failed to clear Redis cache", e);
        }
    }

    @Override
    public boolean containsKey(K key) {
        try {

            return redisTemplate.hasKey(key);
        } catch (DataAccessException e) {
            log.error("Redis CONTAINS KEY check failed for key [{}]: {}", key, e.getMessage(), e);
            throw new CacheException("Failed to check key presence in cache", e);
        }
    }

    /**
     * Example: Atomic batch putIfAbsent for multiple entries.
     * Uses Redis transaction (MULTI/EXEC) to ensure all or nothing.
     */
    public void putMultipleIfAbsent(Map<K, V> entries) {
        try {
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                connection.multi();
                entries.forEach((key, value) -> redisTemplate.opsForValue().setIfAbsent(key, value, ttl));
                connection.exec();
                return null;
            });
        } catch (DataAccessException e) {
            log.error("Redis putMultipleIfAbsent failed: {}", e.getMessage(), e);
            throw new CacheException("Failed to perform batch putIfAbsent", e);
        }
    }

    public Long getHitCount() {
        return hits.get();
    }

    public Long getMissCount() {
        return misses.get();
    }

    /**
     * Thread-safe reset of hit/miss metrics using a lock to prevent partial resets under concurrency.
     */
    public void resetMetrics() {
        metricsLock.lock();
        try {
            hits.set(0);
            misses.set(0);
        } finally {
            metricsLock.unlock();
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down RedisCacheService. Hits: {}, Misses: {}", hits.get(), misses.get());
    }
}
