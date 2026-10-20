package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class HlsStartupPolicyTest {
    @Test public void onlyInitialLiveHlsIsEligible() {
        assertTrue(HlsStartupPolicy.eligible(true,false,false,false,false,false,"application/x-mpegURL"));
        for(int i=0;i<6;i++) {
            assertFalse(HlsStartupPolicy.eligible(i!=0,i==1,i==2,i==3,i==4,i==5,"application/x-mpegURL"));
        }
        assertFalse(HlsStartupPolicy.eligible(true,false,false,false,false,false,"application/dash+xml"));
        assertFalse(HlsStartupPolicy.eligible(true,false,false,false,false,false,null));
    }
    @Test public void fastStartIsOneShotAndDoesNotRelaxLaterSeeks() {
        HlsStartupPolicy p=new HlsStartupPolicy(); p.arm(true);
        assertFalse(p.shouldStart(false,false,true,499999,1f));
        assertTrue(p.shouldStart(false,false,true,500000,1f));
        assertFalse(p.shouldStart(false,false,true,500000,1f));
        assertTrue(p.shouldStart(true,false,true,1500000,1f));
    }
    @Test public void recoveryAndPauseDoNotFastStart() {
        HlsStartupPolicy p=new HlsStartupPolicy(); p.arm(true);
        assertFalse(p.shouldStart(false,false,false,500000,1f));
        assertFalse(p.shouldStart(false,false,true,500000,2f));
        assertFalse(p.shouldStart(false,true,true,500000,1f));
        assertFalse(p.shouldStart(false,false,true,500000,1f));
    }
    @Test public void ordinaryApprovalAndCancellationDisarm() {
        HlsStartupPolicy p=new HlsStartupPolicy(); p.arm(true);
        assertTrue(p.shouldStart(true,false,true,100000,1f));
        assertFalse(p.shouldStart(false,false,true,500000,1f));
        p.arm(true);p.cancel();
        assertFalse(p.shouldStart(false,false,true,500000,1f));
    }
}
