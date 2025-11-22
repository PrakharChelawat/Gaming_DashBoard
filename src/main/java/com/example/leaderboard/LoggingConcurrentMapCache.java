package com.example.leaderboard;

import org.springframework.cache.concurrent.ConcurrentMapCache;

public class LoggingConcurrentMapCache extends ConcurrentMapCache {

    public LoggingConcurrentMapCache(String name) {
        super(name);
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper value = super.get(key);
        if (value == null) {
            System.out.println("Cache MISS for key: " + key);
        } else {
            System.out.println("Cache HIT for key: " + key);
        }
        return value;
    }
}
