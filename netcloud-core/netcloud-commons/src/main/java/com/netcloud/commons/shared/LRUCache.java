package com.netcloud.commons.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Lazy
public class LRUCache<K, V> {

    private final Map<K, V> cache;
    @Value("netcloud.lb.max-memory-utilisation")
    private int maxMemoryUtilisation;
    public LRUCache() {
        this.cache = Collections.synchronizedMap(new LinkedHashMap<K, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                 return  (long) (Runtime.getRuntime().maxMemory() -  Runtime.getRuntime().freeMemory())  > maxMemoryUtilisation;
            }
        });
    }

    public void put(K key, V value) {
        cache.put(key, value);
    }

    public Optional<V> get(K key) {
        return Optional.of(cache.get(key));
    }

    public void remove(K key) {
        cache.remove(key);
    }

    public int size() {
        return cache.size();
    }
}