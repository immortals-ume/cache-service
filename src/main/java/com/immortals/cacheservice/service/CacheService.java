package com.immortals.cacheservice.service;

public interface CacheService<K, V> {
    void put(K key, V value);

    V get(K key);

    void remove(K key);

    void clear();

    boolean containsKey(K key);

    default Long getHitCount() {
        return 0L;
    }

    default Long getMissCount() {
        return 0L;
    }


}
