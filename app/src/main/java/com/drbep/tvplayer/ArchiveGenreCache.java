package com.drbep.tvplayer;
import java.util.*;
/** Small filter list only; never caches pages or delays programme updates. */
final class ArchiveGenreCache {
 interface Loader { List<String> load(); }
 interface Clock { long now(); }
 // Separate from the snapshot monitor: UI peek/get never wait for SQL.
 private final Object loadLock=new Object();
 List<String> getOrLoad(String scope,Clock clock,Loader loader) {
  List<String> cached=get(scope,clock.now());
  if(cached!=null)return cached;
  synchronized(loadLock) {
   cached=get(scope,clock.now());
   if(cached!=null)return cached;
   long generation;
   synchronized(this){generation=revision;loadingScope=scope;}
   try {
    List<String> loaded=loader.load();
    synchronized(this){if(generation==revision)put(scope,clock.now(),loaded);}
    return loaded;
   } finally {
    synchronized(this){loadingScope=null;}
   }
  }
 }
 private long revision;
 private String loadingScope;
 synchronized void invalidate(String changedScope) {
  // Only writes to the active scope can make its in-flight snapshot stale.
  if(changedScope.equals(loadingScope))revision++;
  if(scope.equals(changedScope))expires=Long.MIN_VALUE;
 }
 private String scope="";
 private long expires;
 private List<String> value;
 synchronized List<String> get(String scope,long now) {
  return this.scope.equals(scope) && now<expires ? value : null;
 }
 synchronized List<String> peek(String scope) {
  return this.scope.equals(scope) ? value : null;
 }
 synchronized void put(String scope,long now,List<String> genres) {
  this.scope=scope;expires=now+15000;
  value=Collections.unmodifiableList(new ArrayList<>(genres));
 }
}
