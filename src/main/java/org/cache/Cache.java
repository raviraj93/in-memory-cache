package org.cache;

// Generic Cache Interface
interface Cache<K, V> {
    V get(K key);
    void put(K key, V value);
    void delete(K key);
}
