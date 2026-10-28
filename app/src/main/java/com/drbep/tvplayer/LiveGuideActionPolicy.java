package com.drbep.tvplayer;
final class LiveGuideActionPolicy {
 enum Phase { INVALID, FUTURE, LIVE, PAST, EXPIRED }
 static Phase phase(long start,long end,long now){
  if(start<=0||end<=start)return Phase.INVALID;
  if(start>now)return Phase.FUTURE;
  if(end>now)return Phase.LIVE;
  return now-end>=7L*86400000?Phase.EXPIRED:Phase.PAST;
 }
 static boolean replay(long start,long end,long now,boolean permitted,boolean confirmed){
  Phase p=phase(start,end,now);return permitted&&confirmed&&(p==Phase.LIVE||p==Phase.PAST);
 }
}
