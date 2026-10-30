package com.drbep.tvplayer;
import java.util.function.Supplier;
/** Single visible window only; source replacement and clock refresh invalidate it. */
final class LiveGuideRenderCache<T> {
 private boolean lastHit;
 boolean wasHit(){return lastHit;}
 private Object source; private String key; private long tick; private T value;
 T get(String key,Object source,long tick,Supplier<T> build){
  lastHit=value!=null&&this.source==source&&java.util.Objects.equals(this.key,key)&&this.tick==tick;
  if(!lastHit){
   value=build.get();this.key=key;this.source=source;this.tick=tick;
  }return value;
 }
 void clear(){lastHit=false;source=null;key=null;value=null;}
}
