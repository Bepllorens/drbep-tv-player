package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class CooperativeWorkYieldTest {
    static class Fixture {
        long now,quietUntil=8000; boolean cancelled; int waits,resumes;
        CooperativeWorkYield work=new CooperativeWorkYield(()->now,ms->{now+=ms;},
                ()->{if(cancelled)throw new java.io.InterruptedIOException();},
                ()->Math.max(0,quietUntil-now),15000);
        CooperativeWorkYield.Status status=new CooperativeWorkYield.Status(){
            public void waiting(){waits++;}
            public void resumed(){resumes++;}
        };
    }
    @Test public void idleWorkNeverWaits() throws Exception {
        Fixture f=new Fixture();f.quietUntil=0;
        assertEquals(0,f.work.yield(8000,f.status));assertEquals(0,f.waits);
    }
    @Test public void initialWaitAndBatchWaitAreBounded() throws Exception {
        Fixture f=new Fixture();assertEquals(8000,f.work.yield(8000,f.status));
        f.quietUntil=20000;assertEquals(250,f.work.yield(250,f.status));
        assertEquals(2,f.resumes);
    }
    @Test public void continuousInteractionCannotStarveImport() throws Exception {
        Fixture f=new Fixture();f.quietUntil=Long.MAX_VALUE;
        long waited=f.work.yield(8000,f.status);
        for(int i=0;i<100;i++)waited+=f.work.yield(250,f.status);
        assertEquals(15000,waited);assertEquals(0,f.work.yield(250,f.status));
    }
    @Test public void cancellationAndInterruptionPropagate() throws Exception {
        Fixture f=new Fixture();f.cancelled=true;
        try{f.work.yield(250,f.status);fail();}catch(java.io.InterruptedIOException expected){}
        assertEquals(0,f.waits);
        CooperativeWorkYield w=new CooperativeWorkYield(()->0,ms->{throw new InterruptedException();},
                ()->{},()->1000,1000);
        try{w.yield(250,f.status);fail();}catch(InterruptedException expected){}
        assertEquals(0,f.resumes);
    }
    @Test public void quietEndingEarlyResumesWithoutFullDelay() throws Exception {
        Fixture f=new Fixture();f.quietUntil=120;
        assertEquals(120,f.work.yield(8000,f.status));
    }

    @Test public void metadataBatchRenewingQuietPeriodKeepsCumulativeBudget() throws Exception {
        Fixture f=new Fixture();
        long waited=f.work.yield(8000,f.status);
        for(int i=0;i<100;i++) {
            f.quietUntil=f.now+750;
            waited+=f.work.yield(250,f.status);
        }
        assertEquals(15000,waited);
    }
    @Test public void cancellationStillCheckedWhenBudgetExhausted() throws Exception {
        Fixture f=new Fixture();f.quietUntil=Long.MAX_VALUE;
        f.work.yield(15000,f.status);f.cancelled=true;
        try{f.work.yield(250,f.status);fail();}catch(java.io.InterruptedIOException expected){}
    }
}
