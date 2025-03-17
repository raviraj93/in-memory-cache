package org.cache;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PerpexilityTransactionTest {

    private PerpexilityTransaction<String, Integer> cache;

    @BeforeEach
    public void setUp() {
        cache = new PerpexilityTransaction<>();
    }

    @Test
    public void testBasicOperations() {
        // Put and get
        cache.put("key1", 100);
        assertEquals(100, cache.get("key1"));

        // Update value
        cache.put("key1", 200);
        assertEquals(200, cache.get("key1"));

        // Delete
        cache.delete("key1");
        assertNull(cache.get("key1"));
    }

    @Test
    public void testSingleTransaction() {
        cache.put("key1", 100);

        // Begin transaction
        cache.beginTransaction();

        // Modify within transaction
        cache.put("key1", 200);
        assertEquals(200, cache.get("key1"));

        // Commit transaction
        cache.commitTransaction();
        assertEquals(200, cache.get("key1"));
    }

    @Test
    public void testTransactionRollback() {
        cache.put("key1", 100);

        // Begin transaction
        cache.beginTransaction();

        // Modify within transaction
        cache.put("key1", 200);
        assertEquals(200, cache.get("key1"));

        // Rollback transaction
        cache.rollbackTransaction();
        assertEquals(100, cache.get("key1"));
    }

    @Test
    public void testNestedTransactions() {
        cache.put("key1", 100);

        // Begin parent transaction
        cache.beginTransaction();
        cache.put("key1", 200);

        // Begin child transaction
        cache.beginTransaction();
        cache.put("key1", 300);
        assertEquals(300, cache.get("key1"));

        // Rollback child transaction
        cache.rollbackTransaction();
        assertEquals(200, cache.get("key1"));

        // Commit parent transaction
        cache.commitTransaction();
        assertEquals(200, cache.get("key1"));
    }

    @Test
    public void testRollbackWithDelete() {
        cache.put("key1", 100);

        cache.beginTransaction();
        cache.delete("key1");
        assertNull(cache.get("key1"));

        cache.rollbackTransaction();
        assertEquals(100, cache.get("key1"));
    }

    @Test
    public void testRollbackAfterMultipleChanges() {
        // Setup initial state
        cache.put("key1", 100);
        cache.put("key2", 200);

        cache.beginTransaction();
        cache.put("key1", 150);
        cache.delete("key2");
        cache.put("key3", 300);

        assertEquals(150, cache.get("key1"));
        assertNull(cache.get("key2"));
        assertEquals(300, cache.get("key3"));

        cache.rollbackTransaction();

        assertEquals(100, cache.get("key1"));
        assertEquals(200, cache.get("key2"));
        assertNull(cache.get("key3"));
    }

    @Test
    public void testExceptionOnInvalidCommit() {
        assertThrows(IllegalStateException.class, () -> {
            cache.commitTransaction();
        });
    }

    @Test
    public void testExceptionOnInvalidRollback() {
        assertThrows(IllegalStateException.class, () -> {
            cache.rollbackTransaction();
        });
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        final int threadCount = 10;
        final ExecutorService executorService = newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    cache.beginTransaction();
                    cache.put("key" + index, index);
                    cache.commitTransaction();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executorService.shutdown();

        for (int i = 0; i < threadCount; i++) {
            assertEquals(i, cache.get("key" + i));
        }
    }

    @Test
    public void testConcurrentTransactionRollback() throws InterruptedException {
        cache.put("shared", 0);

        final int threadCount = 5;
        final ExecutorService executorService = newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    cache.beginTransaction();
                    int current = cache.get("shared");
                    cache.put("shared", current + 10);
                    // Randomly decide whether to commit or rollback
                    if (Math.random() < 0.5) {
                        cache.commitTransaction();
                    } else {
                        cache.rollbackTransaction();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executorService.shutdown();

        // The final value should be a multiple of 10 because each committed transaction adds 10
        int finalValue = cache.get("shared");
        assertEquals(0, finalValue % 10);
    }
}
