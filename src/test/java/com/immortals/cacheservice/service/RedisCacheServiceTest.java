package com.immortals.cacheservice.service;

import com.immortals.cacheservice.service.exception.CacheException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Captor
    private ArgumentCaptor<RedisCallback<Void>> callbackCaptor;

    private RedisCacheService<String, String> cacheService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new RedisCacheService<>(redisTemplate, Duration.ofSeconds(100));
    }

    @Test
    void put_shouldStoreValue() {
        doNothing().when(valueOperations).set("key", "value", Duration.ofSeconds(100));
        assertDoesNotThrow(() -> cacheService.put("key", "value"));
        verify(valueOperations).set("key", "value", Duration.ofSeconds(100));
    }

    @Test
    void put_shouldThrowOnException() {
        doThrow(new DataAccessException("fail") {
        }).when(valueOperations).set(any(), any(), any());
        CacheException ex = assertThrows(CacheException.class, () -> cacheService.put("k", "v"));
        assertTrue(ex.getMessage().contains("Failed to put value in cache"));
    }

    @Test
    void get_shouldReturnValueAndCountHits() {
        when(valueOperations.get("key")).thenReturn("value");
        String value = cacheService.get("key");
        assertEquals("value", value);
        assertEquals(1L, cacheService.getHitCount());
        assertEquals(0L, cacheService.getMissCount());
    }

    @Test
    void get_shouldCountMisses() {
        when(valueOperations.get("key")).thenReturn(null);
        assertNull(cacheService.get("key"));
        assertEquals(0L, cacheService.getHitCount());
        assertEquals(1L, cacheService.getMissCount());
    }

    @Test
    void get_shouldThrowOnExceptionAndCountMisses() {
        when(valueOperations.get("err")).thenThrow(new DataAccessException("fail") {
        });
        assertThrows(CacheException.class, () -> cacheService.get("err"));
        assertEquals(0L, cacheService.getHitCount());
        assertEquals(1L, cacheService.getMissCount());
    }

    @Test
    void putIfAbsent_success() {
        when(valueOperations.setIfAbsent("key", "value", Duration.ofSeconds(100))).thenReturn(Boolean.TRUE);
        assertTrue(cacheService.putIfAbsent("key", "value"));
        assertEquals(1L, cacheService.getHitCount());
        assertEquals(0L, cacheService.getMissCount());
    }

    @Test
    void putIfAbsent_fail() {
        when(valueOperations.setIfAbsent("key", "value", Duration.ofSeconds(100))).thenReturn(Boolean.FALSE);
        assertFalse(cacheService.putIfAbsent("key", "value"));
        assertEquals(0L, cacheService.getHitCount());
        assertEquals(1L, cacheService.getMissCount());
    }

    @Test
    void putIfAbsent_exception() {
        when(valueOperations.setIfAbsent("key", "value", Duration.ofSeconds(100))).thenThrow(new DataAccessException("fail") {
        });
        assertThrows(CacheException.class, () -> cacheService.putIfAbsent("key", "value"));
        assertEquals(0L, cacheService.getHitCount());
        assertEquals(1L, cacheService.getMissCount());
    }

    @Test
    void remove_shouldDeleteKey() {
        when(redisTemplate.delete("key")).thenReturn(true);
        assertDoesNotThrow(() -> cacheService.remove("key"));
        verify(redisTemplate).delete("key");
    }

    @Test
    void remove_shouldThrowOnException() {
        doThrow(new DataAccessException("fail") {
        }).when(redisTemplate).delete("key");
        assertThrows(CacheException.class, () -> cacheService.remove("key"));
    }

    @Test
    void clear_shouldExecuteCallback() {
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(null);
        assertDoesNotThrow(() -> cacheService.clear());
        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    void clear_shouldThrowOnException() {
        when(redisTemplate.execute(any(RedisCallback.class))).thenThrow(new DataAccessException("fail") {
        });
        assertThrows(CacheException.class, () -> cacheService.clear());
    }

    @Test
    void containsKey_shouldReturnTrue() {
        when(redisTemplate.hasKey("key")).thenReturn(true);
        assertTrue(cacheService.containsKey("key"));
    }

    @Test
    void containsKey_shouldReturnFalse() {
        when(redisTemplate.hasKey("key")).thenReturn(false);
        assertFalse(cacheService.containsKey("key"));
    }

    @Test
    void containsKey_shouldThrowOnException() {
        when(redisTemplate.hasKey(any())).thenThrow(new DataAccessException("fail") {
        });
        assertThrows(CacheException.class, () -> cacheService.containsKey("key"));
    }

    @Test
    void putMultipleIfAbsent_success() {
        Map<String, String> data = new HashMap<>();
        data.put("k1", "v1");
        data.put("k2", "v2");
        // Simulate transaction
        when(redisTemplate.execute(any(RedisCallback.class))).then(invocation -> {
            RedisCallback<?> cb = invocation.getArgument(0);
            cb.doInRedis(mock(org.springframework.data.redis.connection.RedisConnection.class));
            return null;
        });
        assertDoesNotThrow(() -> cacheService.putMultipleIfAbsent(data));
        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    void putMultipleIfAbsent_exception() {
        Map<String, String> data = new HashMap<>();
        data.put("k1", "v1");
        when(redisTemplate.execute(any(RedisCallback.class))).thenThrow(new DataAccessException("fail") {
        });
        assertThrows(CacheException.class, () -> cacheService.putMultipleIfAbsent(data));
    }

    @Test
    void resetMetrics_shouldResetCountsThreadSafe() {
        cacheService.putIfAbsent("x", "y");
        cacheService.get("absent");
        assertTrue(cacheService.getHitCount() > 0 || cacheService.getMissCount() > 0);
        cacheService.resetMetrics();
        assertEquals(0L, cacheService.getHitCount());
        assertEquals(0L, cacheService.getMissCount());
    }
}