package org.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransactionalCacheTest {
    private TransactionalCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = new TransactionalCache<>();
    }

    @Test
    void testTransactionRollbackRestoresOldValues() {
        cache.put("key1", "originalValue");

        cache.beginTransaction();
        cache.put("key1", "newValue");
        cache.put("key2", "value2");
        cache.rollbackTransaction();

        assertEquals("originalValue", cache.get("key1"), "Rollback should restore the original value.");
        assertNull(cache.get("key2"), "Rollback should remove the inserted key.");
    }

    @Test
    void testTransactionCommitAppliesChanges() {
        cache.put("key1", "originalValue");

        cache.beginTransaction();
        cache.put("key1", "newValue");
        cache.put("key2", "value2");
        cache.commitTransaction();

        assertEquals("newValue", cache.get("key1"), "Commit should apply the new value.");
        assertEquals("value2", cache.get("key2"), "Commit should retain newly added values.");
    }

    @Test
    void testDirectUpdateWithoutTransactionShouldApplyImmediately() {
        cache.put("key1", "directValue");
        cache.delete("key2");

        assertEquals("directValue", cache.get("key1"), "Direct put should apply immediately.");
        assertNull(cache.get("key2"), "Direct delete should apply immediately.");
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
    void testBeginTransactionWhileAnotherIsActiveShouldThrowError() {
        cache.beginTransaction();
        Exception exception = assertThrows(IllegalStateException.class, cache::beginTransaction);
        assertEquals("A transaction is already in progress", exception.getMessage());
    }
}
