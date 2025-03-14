package org.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

class NestedTransactionCache<K, V> implements Cache<K, V> {
    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<ConcurrentLinkedDeque<CacheCommand<K, V>>> transactionStack = new ConcurrentLinkedDeque<>();

    @Override
    public V get(K key) {
        return cache.get(key);
    }

    @Override
    public void put(K key, V value) {
        if (!transactionStack.isEmpty()) {
            V oldValue = cache.get(key);
            transactionStack.peek().push(new PutCommand<>(key, value, oldValue));
        }
        cache.put(key, value);
    }

    @Override
    public void delete(K key) {
        if (!transactionStack.isEmpty()) {
            V oldValue = cache.get(key);
            transactionStack.peek().push(new DeleteCommand<>(key, oldValue));
        }
        cache.remove(key);
    }

    public void beginTransaction() {
        transactionStack.push(new ConcurrentLinkedDeque<>()); // Create a new transaction level
    }

    public void commitTransaction() {
        if (transactionStack.isEmpty()) {
            throw new IllegalStateException("No active transaction to commit");
        }
        ConcurrentLinkedDeque<CacheCommand<K, V>> completedTransaction = transactionStack.pop();

        // If a parent transaction exists, merge it with the parent
        if (!transactionStack.isEmpty()) {
            transactionStack.peek().addAll(completedTransaction);
        }
    }

    public void rollbackTransaction() {
        if (transactionStack.isEmpty()) {
            throw new IllegalStateException("No active transaction to rollback");
        }

        // If any transaction fails, rollback all active transactions
        while (!transactionStack.isEmpty()) {
            ConcurrentLinkedDeque<CacheCommand<K, V>> currentTransaction = transactionStack.pop();
            while (!currentTransaction.isEmpty()) {
                currentTransaction.pop().undo(cache);
            }
        }
    }

}
