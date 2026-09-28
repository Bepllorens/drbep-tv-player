package com.drbep.tvplayer.netflix;

import java.util.Arrays;

/** Estimates video cadence from presentation timestamps when the container omits frameRate. */
final class NetflixFrameRate {
    private final long[] deltas=new long[31];
    private long previous=Long.MIN_VALUE;
    private int count,index;
    synchronized void reset(){previous=Long.MIN_VALUE;count=0;index=0;}
    synchronized void record(long presentationUs){
        if(previous==Long.MIN_VALUE){previous=presentationUs;return;}
        long delta=presentationUs-previous;
        previous=presentationUs;
        if(delta==0)return;
        if(delta<4000 || delta>500000){count=0;index=0;return;}
        deltas[index]=delta;index=(index+1)%deltas.length;count=Math.min(count+1,deltas.length);
    }
    synchronized float estimate(){
        if(count<8)return -1;
        long[] sorted=Arrays.copyOf(deltas,count);Arrays.sort(sorted);
        double median=count%2==0?(sorted[count/2-1]+sorted[count/2])/2.0:sorted[count/2];
        return (float)(1000000.0/median);
    }
}
