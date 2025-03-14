package org.cache;

public class Main {
    public static void main(String[] args) {
        TransactionalCache<String, String> cache = new TransactionalCache<>();

        // Begin a transaction
        cache.beginTransaction();
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.delete("key1");

        cache.beginTransaction();
        cache.put("key1", "newValue1");
        System.out.println("Before rollback: key1=" + cache.get("key1") + ", key2=" + cache.get("key2"));

        // Rollback transaction
        cache.rollbackTransaction();
        System.out.println("After rollback: key1=" + cache.get("key1") + ", key2=" + cache.get("key2"));

        // Begin another transaction and commit
        cache.beginTransaction();
        cache.put("key1", "newValue1");
        cache.commitTransaction();

        System.out.println("After commit: key1=" + cache.get("key1"));
    }
}
