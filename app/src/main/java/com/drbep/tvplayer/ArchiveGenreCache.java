package com.drbep.tvplayer;
import java.util.*;
/** Small filter list only; never caches pages or delays programme updates. */
final class ArchiveGenreCache {
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
  value=genres.isEmpty()?null:Collections.unmodifiableList(new ArrayList<>(genres));
 }
}
