package com.immortals.cacheservice.service.exception;


public class CacheException extends RuntimeException {
    public CacheException(String message, RuntimeException e) {
        super(message, e);
    }
}
