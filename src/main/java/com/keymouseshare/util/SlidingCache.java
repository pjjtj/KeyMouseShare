package com.keymouseshare.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class SlidingCache<K, V> {

    private class CacheObject {
        V value;
        long expiryTime; // 到期时间戳

        CacheObject(V value, long ttlMillis) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttlMillis;
        }

        void refresh(long ttlMillis) {
            this.expiryTime = System.currentTimeMillis() + ttlMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    private final ConcurrentSkipListMap<K, CacheObject> cache = new ConcurrentSkipListMap<>();
    private final long ttlMillis;
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    public SlidingCache(long ttlMillis) {
        this.ttlMillis = ttlMillis;
        cleaner.scheduleAtFixedRate(this::cleanup, ttlMillis, ttlMillis, TimeUnit.MILLISECONDS);
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }

    public void put(K key, V value) {
        if (cache.containsKey(key)) {
            cache.get(key);
        } else {
            cache.put(key, new CacheObject(value, ttlMillis));
        }
    }


    /**
     * 获取缓存中所有的值
     *
     * @return 包含所有未过期值的列表
     */
    public List<V> getValues() {
        List<V> values = new ArrayList<>();
        for (K key : cache.keySet()) {
            V value = get(key);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    /**
     * 获取缓存中所有的键值对
     *
     * @return 包含所有未过期键值对的列表
     */
    public List<K> getKeys() {
        List<K> keys = new ArrayList<>();
        for (K key : cache.keySet()) {
            V value = get(key);
            if (value != null) {
                keys.add(key);
            }
        }
        return keys;
    }

    public V get(K key) {
        CacheObject obj = cache.get(key);
        if (obj == null || obj.isExpired()) {
            cache.remove(key);
            return null;
        }
        obj.refresh(ttlMillis); // 每次访问刷新过期时间
        return obj.value;
    }

    public void remove(K key) {
        cache.remove(key);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        for (K key : cache.keySet()) {
            CacheObject obj = cache.get(key);
            if (obj != null && obj.isExpired()) {
                cache.remove(key);
            }
        }
    }

    public void shutdown() {
        cleaner.shutdown();
    }
}