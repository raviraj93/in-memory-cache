package org.cache;

import java.util.Map;

record DeleteCommand<K, V>(K key, V oldValue) implements CacheCommand<K, V> {
    @Override
    public void execute(Map<K, V> cache) {
        cache.remove(key);
    }

    @Override
    public void undo(Map<K, V> cache) {
        if (oldValue != null) {
            cache.put(key, oldValue);
        }
    }
}