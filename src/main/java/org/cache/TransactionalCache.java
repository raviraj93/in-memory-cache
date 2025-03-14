package org.cache;

import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

class TransactionalCache<K, V> implements Cache<K, V> {
    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private final Stack<CacheCommand<K, V>> transactionStack = new Stack<>();
    private boolean inTransaction = false; // Flag to track active transactions

    @Override
    public V get(K key) {
        return cache.get(key);
    }

    @Override
    public void put(K key, V value) {
        if (inTransaction) {
            V oldValue = cache.get(key);
            transactionStack.push(new PutCommand<>(key, value, oldValue));
        }
        cache.put(key, value);
    }

    @Override
    public void delete(K key) {
        if (inTransaction) {
            V oldValue = cache.get(key);
            transactionStack.push(new DeleteCommand<>(key, oldValue));
        }
        cache.remove(key);
    }

    public void beginTransaction() {
        if (inTransaction) {
            throw new IllegalStateException("A transaction is already in progress");
        }
        inTransaction = true;
        transactionStack.clear();
    }

    public void commitTransaction() {
        if (!inTransaction) {
            throw new IllegalStateException("No active transaction to commit");
        }
        inTransaction = false;
        transactionStack.clear(); // Clear rollback history since we are committing
    }

    public void rollbackTransaction() {
        if (!inTransaction) {
            throw new IllegalStateException("No active transaction to rollback");
        }
        while (!transactionStack.isEmpty()) {
            transactionStack.pop().undo(cache);
        }
        inTransaction = false;
    }
}
