package com.trideveloper.exhibit;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

class Cache<K, V> {

    private static final Timer TIMER = new Timer(true);

    private final Map<K, TimerTask> taskMap;

    private final long timeout;

    private final Map<K, V> cache;

    public Cache(long cacheTimeout, int cacheSize) {
        if (cacheTimeout <= 0l) cacheTimeout = 0l;
        this.timeout = cacheTimeout;
        this.taskMap = (cacheTimeout > 0l) ? new HashMap<K, TimerTask>() : null;
        final int size = cacheSize;
        this.cache = new LinkedHashMap<K, V>(size, 1.0f, true) {
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                if (size() < size - 1) return false;
                if (timeout > 0l) {
                    TimerTask task = taskMap.remove(eldest.getKey());
                    if (task != null) task.cancel();
                }
                return true;
            }
        };
    }

    public V get(K key) {
        synchronized (this) {
            return cache.get(key);
        }
    }

    public void put(K key, V value) {
        synchronized (this) {
            cache.put(key, value);
            if (timeout > 0l) {
                final K index = key;
                TimerTask task = new TimerTask() {
                    public void run() {
                        synchronized (Cache.this) {
                            cache.remove(index);
                            taskMap.remove(index);
                        }
                    }
                };
                TimerTask oldTask = taskMap.put(key, task);
                if (oldTask != null) oldTask.cancel();
                TIMER.schedule(task, timeout);
            }
        }
    }

}
