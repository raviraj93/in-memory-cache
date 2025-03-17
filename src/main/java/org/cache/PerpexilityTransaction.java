package org.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class PerpexilityTransaction<K, V> implements Cache<K, V> {
    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<ConcurrentLinkedDeque<Command<K, V>>> transactionStack = new ConcurrentLinkedDeque<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public V get(K key) {
        lock.readLock().lock();
        try {
            // Check for uncommitted changes in the current transaction
            if (!transactionStack.isEmpty()) {
                ConcurrentLinkedDeque<Command<K, V>> currentTransaction = transactionStack.peek();
                if (currentTransaction != null) {
                    for (Command<K, V> command : currentTransaction) {
                        if (command.getKey().equals(key)) {
                            return command.getCurrentValue();
                        }
                    }
                }
            }
            return cache.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void put(K key, V value) {
        lock.writeLock().lock();
        try {
            if (!transactionStack.isEmpty()) {
                ConcurrentLinkedDeque<Command<K, V>> currentTransaction = transactionStack.peek();
                if (currentTransaction != null) {
                    V oldValue = cache.get(key);
                    currentTransaction.add(new Put<>(key, value, oldValue));
                }
            }
            cache.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(K key) {
        lock.writeLock().lock();
        try {
            if (!transactionStack.isEmpty()) {
                ConcurrentLinkedDeque<Command<K, V>> currentTransaction = transactionStack.peek();
                if (currentTransaction != null) {
                    V oldValue = cache.get(key);
                    currentTransaction.add(new Delete<>(key, oldValue));
                }
            }
            cache.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void beginTransaction() {
        lock.writeLock().lock();
        try {
            transactionStack.push(new ConcurrentLinkedDeque<>()); // Create a new transaction level
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void commitTransaction() {
        lock.writeLock().lock();
        try {
            if (transactionStack.isEmpty()) {
                throw new IllegalStateException("No active transaction to commit");
            }
            ConcurrentLinkedDeque<Command<K, V>> completedTransaction = transactionStack.pop();

            // If a parent transaction exists, merge it with the parent
            if (!transactionStack.isEmpty()) {
                transactionStack.peek().addAll(completedTransaction);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void rollbackTransaction() {
        lock.writeLock().lock();
        try {
            if (transactionStack.isEmpty()) {
                throw new IllegalStateException("No active transaction to rollback");
            }

            // Roll back only the current transaction
            ConcurrentLinkedDeque<Command<K, V>> currentTransaction = transactionStack.pop();
            while (!currentTransaction.isEmpty()) {
                try {
                    currentTransaction.pollLast().undo(cache); // Use pollLast to reverse order
                } catch (Exception e) {
                    // Log the exception but continue rollback
                    System.err.println("Error during rollback: " + e.getMessage());
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}

// Command interfaces and implementations
interface Command<K, V> {
    void undo(Map<K, V> cache);
    K getKey();
    V getCurrentValue();
}

class Put<K, V> implements Command<K, V> {
    private final K key;
    private final V newValue;
    private final V oldValue;


    public Put(K key, V newValue, V oldValue) {
        this.key = key;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    @Override
    public void undo(Map<K, V> cache) {
        if (oldValue != null) {
            cache.put(key, oldValue);
        } else {
            cache.remove(key);
        }
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getCurrentValue() {
        return newValue;
    }
}

class Delete<K, V> implements Command<K, V> {
    private final K key;
    private final V oldValue;

    public Delete(K key, V oldValue) {
        this.key = key;
        this.oldValue = oldValue;
    }

    @Override
    public void undo(Map<K, V> cache) {
        if (oldValue != null) {
            cache.put(key, oldValue);
        }
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getCurrentValue() {
        return null;
    }
}
