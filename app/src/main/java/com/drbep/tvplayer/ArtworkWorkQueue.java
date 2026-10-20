package com.drbep.tvplayer;
import java.util.concurrent.*;
/** Bounds optional artwork work; never executes network work on the submitting thread. */
final class ArtworkWorkQueue {
 static ExecutorService create(int parallelism,int capacity) {
  if(parallelism<1||capacity<1)throw new IllegalArgumentException();
  return new ThreadPoolExecutor(parallelism,parallelism,0,TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(capacity),new ThreadPoolExecutor.AbortPolicy());
 }
 static boolean submit(Executor executor,Runnable job) {
  try{executor.execute(job);return true;}catch(RejectedExecutionException rejected){return false;}
 }
 static Object[] locks(int count) {
  if(count<1)throw new IllegalArgumentException();
  Object[] locks=new Object[count];for(int i=0;i<count;i++)locks[i]=new Object();return locks;
 }
 static Object lock(Object[] locks,String key){return locks[(key.hashCode()&0x7fffffff)%locks.length];}
}
