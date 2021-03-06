package com.github.codinghck.base.util.common.cache;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 用来存储短暂对象的缓存类，实现Map接口，内部有一个定时器用来清除过期（30秒）的对象。
 * 为避免创建过多线程，没有特殊要求请使用getDefault()方法来获取本类的实例
 *
 * @author hck 2018/11/29 10:12 AM
 */
@SuppressWarnings("unused")
public class CacheMap<K, V> extends AbstractMap<K, V> {

  private static final long DEFAULT_TIMEOUT = 30000;
  private static CacheMap<Object, Object> defaultInstance;

  public static CacheMap<Object, Object> getDefault() {
    return getDefault(DEFAULT_TIMEOUT);
  }

  public static synchronized CacheMap<Object, Object> getDefault(long millis) {
    if (defaultInstance == null) {
      defaultInstance = new CacheMap<>(millis);
    }
    return defaultInstance;
  }

  @ToString
  @EqualsAndHashCode
  private class CacheEntry implements Entry<K, V> {
    long time;
    V value;
    K key;

    CacheEntry(K key, V value) {
      super();
      this.value = value;
      this.key = key;
      this.time = System.currentTimeMillis();
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public V setValue(V value) {
      return this.value = value;
    }
  }

  private class ClearThread extends Thread {
    ClearThread() {
      setName("clear cache thread");
    }

    @Override
//    @SuppressWarnings("all")
    public void run() {
      while (true) {
          long now = System.currentTimeMillis();
          Object[] keys = map.keySet().toArray();
          for (Object key : keys) {
            CacheEntry entry = map.get(key);
            if (now - entry.time >= cacheTimeout) {
              synchronized (map) {
                map.remove(key);
              }
            }
          }
        try {
          Thread.sleep(cacheTimeout);
        } catch (InterruptedException ignored) {

        }
      }
    }
  }

  private long cacheTimeout;
  private final Map<K, CacheEntry> map = new HashMap<>();

  private CacheMap(long timeout) {
    this.cacheTimeout = timeout;
    new ClearThread().start();
  }

  @NotNull
  @Override
  public Set<Entry<K, V>> entrySet() {
    Set<Entry<K, V>> entrySet = new HashSet<>();
    Set<Entry<K, CacheEntry>> wrapEntrySet = map.entrySet();
    for (Entry<K, CacheEntry> entry : wrapEntrySet) {
      entrySet.add(entry.getValue());
    }
    return entrySet;
  }

  @Override
  public V get(Object key) {
    CacheEntry entry = map.get(key);
    return entry == null ? null : entry.value;
  }

  @Override
  public V put(K key, V value) {
    CacheEntry entry = new CacheEntry(key, value);
    synchronized (map) {
      map.put(key, entry);
    }
    return value;
  }
}
