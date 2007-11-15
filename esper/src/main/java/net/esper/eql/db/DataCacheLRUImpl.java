package net.esper.eql.db;

import net.esper.collection.MultiKey;
import net.esper.eql.join.table.EventTable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Query result data cache implementation that uses a least-recently-used algorithm
 * to store and evict query results.
 */
public class DataCacheLRUImpl implements DataCache
{
    private final int cacheSize;
    private static final float hashTableLoadFactor = 0.75f;
    private final LinkedHashMap<MultiKey<Object>, EventTable> cache;

    /**
     * Ctor.
     * @param cacheSize is the maximum cache size
     */
    public DataCacheLRUImpl(int cacheSize)
    {
        this.cacheSize = cacheSize;
        int hashTableCapacity = (int)Math.ceil(cacheSize / hashTableLoadFactor) + 1;
        this.cache = new LinkedHashMap<MultiKey<Object>,EventTable>(hashTableCapacity, hashTableLoadFactor, true)
        {
            private static final long serialVersionUID = 1;

            @Override protected boolean removeEldestEntry (Map.Entry<MultiKey<Object>,EventTable> eldest)
            {
                return size() > DataCacheLRUImpl.this.cacheSize;
            }
        };
    }

    /**
    * Retrieves an entry from the cache.
    * The retrieved entry becomes the MRU (most recently used) entry.
    * @param lookupKeys the key whose associated value is to be returned.
    * @return the value associated to this key, or null if no value with this key exists in the cache.
    */
    public EventTable getCached(Object[] lookupKeys)
    {
        MultiKey<Object> keys = new MultiKey<Object>(lookupKeys);
        return cache.get(keys);
    }

    /**
    * Adds an entry to this cache.
    * If the cache is full, the LRU (least recently used) entry is dropped.
    * @param key the key with which the specified value is to be associated.
    * @param value a value to be associated with the specified key.
    */
    public synchronized void put(Object[] key, EventTable value)
    {
        MultiKey<Object> mkeys = new MultiKey<Object>(key);
        cache.put(mkeys, value);
    }

    /**
     * Returns the maximum cache size.
     * @return maximum cache size
     */
    public int getCacheSize()
    {
        return cacheSize;
    }

    public boolean isActive()
    {
        return true;
    }
}
