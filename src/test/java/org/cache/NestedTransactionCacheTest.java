package org.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NestedTransactionCacheTest {
    private NestedTransactionCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = new NestedTransactionCache<>();
    }

    @Test
    void testNestedTransactionCommitMergesWithParent() {
        cache.put("key1", "original");

        cache.beginTransaction();
        cache.put("key1", "transaction1");

        cache.beginTransaction();
        cache.put("key1", "transaction2");
        cache.commitTransaction(); // Commit inner transaction

        assertEquals("transaction2", cache.get("key1"), "Nested transaction should be merged");

        cache.commitTransaction(); // Commit outer transaction
        assertEquals("transaction2", cache.get("key1"), "Final commit should persist changes");
    }

    @Test
    void testNestedTransactionRollbackRollsBackEverything() {
        cache.put("key1", "original");

        cache.beginTransaction();
        cache.put("key1", "transaction1");

        cache.beginTransaction();
        cache.put("key1", "transaction2");
        cache.rollbackTransaction(); // Should rollback everything, not just inner

        assertEquals("original", cache.get("key1"), "Rollback should revert all transactions");
    }

    @Test
    void testMultipleNestedTransactionsWithCommitAndRollback() {
        cache.put("key1", "original");

        cache.beginTransaction(); // Start outer transaction
        cache.put("key1", "transaction1");

        cache.beginTransaction(); // Start first nested transaction
        cache.put("key1", "transaction2");
        cache.commitTransaction(); // Commit first nested transaction (transaction2 should persist in the outer transaction)

        cache.beginTransaction(); // Start second nested transaction
        cache.put("key1", "transaction3");
        cache.rollbackTransaction(); // Should rollback the entire transaction

        assertEquals("original", cache.get("key1"), "Rollback at any level should rollback everything.");
    }


    @Test
    void testRollbackWithoutTransactionShouldThrowError() {
        Exception exception = assertThrows(IllegalStateException.class, cache::rollbackTransaction);
        assertEquals("No active transaction to rollback", exception.getMessage());
    }

    @Test
    void testCommitWithoutTransactionShouldThrowError() {
        Exception exception = assertThrows(IllegalStateException.class, cache::commitTransaction);
        assertEquals("No active transaction to commit", exception.getMessage());
    }

    @Test
    void testBeginTransactionWhileAnotherIsActiveShouldAllowNesting() {
        cache.beginTransaction();
        cache.beginTransaction(); // Should allow nesting, no error
        cache.commitTransaction(); // Inner commit
        cache.commitTransaction(); // Outer commit
    }

}
