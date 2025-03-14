package org.cache;

import java.util.Map;

interface CacheCommand<K, V> {
    void execute(Map<K, V> cache);
    void undo(Map<K, V> cache);
}
