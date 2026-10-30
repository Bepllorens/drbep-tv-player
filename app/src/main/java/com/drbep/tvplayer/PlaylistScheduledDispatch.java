package com.drbep.tvplayer;
/** Uses the same monitor as manual enqueue and configuration, with a fresh decision inside it. */
final class PlaylistScheduledDispatch {
 interface Source {
  boolean due() throws Exception;
  void enqueue() throws Exception;
  void markHandled() throws Exception;
 }
 static boolean run(Object lock,Source source)throws Exception {
  synchronized(lock) {
   if(!source.due())return false;
   source.enqueue();
   source.markHandled();
   return true;
  }
 }
}
