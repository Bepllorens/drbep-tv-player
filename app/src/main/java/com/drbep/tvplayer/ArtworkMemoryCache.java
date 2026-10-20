package com.drbep.tvplayer;
import java.util.LinkedHashMap;
import java.util.Map;
/** Small artwork lookup cache; trim never waits for I/O or resurrects old loads. */
final class ArtworkMemoryCache<V> {
 private final int capacity;
 private final Map<String,V> values=new LinkedHashMap<>(32,.75f,true);
 private long generation;
 ArtworkMemoryCache(int capacity){if(capacity<1)throw new IllegalArgumentException();this.capacity=capacity;}
 synchronized V get(String key){return values.get(key);}
 synchronized long generation(){return generation;}
 synchronized void putIfCurrent(String key,V value,long startedGeneration){
  if(startedGeneration!=generation)return;
  values.put(key,value);
  while(values.size()>capacity)values.remove(values.keySet().iterator().next());
 }
 synchronized void clear(){generation++;values.clear();}
 synchronized int size(){return values.size();}
}
