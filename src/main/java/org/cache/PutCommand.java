package org.cache;

import java.util.Map;

record PutCommand<K, V>(K key, V value, V oldValue) implements CacheCommand<K, V> {
    @Override
    public void execute(Map<K, V> cache) {
        cache.put(key, value);
    }

    @Override
    public void undo(Map<K, V> cache) {
        if (oldValue == null) {
            cache.remove(key);
        } else {
            cache.put(key, oldValue);
        }
    }
}