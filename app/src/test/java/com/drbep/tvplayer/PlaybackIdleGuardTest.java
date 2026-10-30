package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlaybackIdleGuardTest {
    private static final long HOUR=3_600_000L;
    @Test public void fourHoursThenOneMinuteWarning() {
        PlaybackIdleGuard guard=new PlaybackIdleGuard(0);
        guard.playback(true,0);
        assertEquals(PlaybackIdleGuard.Event.NONE,guard.poll(4*HOUR-1));
        assertEquals(PlaybackIdleGuard.Event.WARN,guard.poll(4*HOUR));
        assertEquals(60,guard.warningRemainingSeconds(4*HOUR));
        assertEquals(PlaybackIdleGuard.Event.STOP_IDLE,guard.poll(4*HOUR+60_000));
        assertEquals(PlaybackIdleGuard.Event.NONE,guard.poll(9*HOUR));
    }
    @Test public void channelChangesAndBackgroundWorkDoNotCountAsInput() {
        PlaybackIdleGuard guard=new PlaybackIdleGuard(0);
        guard.playback(true,0);guard.playback(true,3*HOUR);
        assertEquals(PlaybackIdleGuard.Event.WARN,guard.poll(4*HOUR));
    }
    @Test public void actualInputRenewsTheWindow() {
        PlaybackIdleGuard guard=new PlaybackIdleGuard(0);
        guard.playback(true,0);guard.interaction(4*HOUR+10_000);
        assertEquals(PlaybackIdleGuard.Event.NONE,guard.poll(4*HOUR+59_000));
        assertEquals(PlaybackIdleGuard.Event.WARN,guard.poll(8*HOUR+10_000));
    }
    @Test public void doesNotStopAnIdleMenuThatHasNeverPlayed() {
        PlaybackIdleGuard guard=new PlaybackIdleGuard(0);
        assertEquals(PlaybackIdleGuard.Event.NONE,guard.poll(48*HOUR));
    }
    @Test public void newPlaybackStartsWithItsOwnIdleWindow() {
        PlaybackIdleGuard guard=new PlaybackIdleGuard(0);
        guard.playback(true,10*HOUR);
        assertEquals(PlaybackIdleGuard.Event.NONE,guard.poll(10*HOUR+1));
        assertEquals(PlaybackIdleGuard.Event.WARN,guard.poll(14*HOUR));
    }
    @Test public void outputLossIsDebouncedAndReconnectDoesNotStop() {
        PlaybackIdleGuard guard=new PlaybackIdleGuard(0);
        guard.playback(true,0);guard.output(false,1000);
        assertEquals(PlaybackIdleGuard.Event.NONE,guard.poll(20_000));
        guard.output(true,20_500);
        assertEquals(PlaybackIdleGuard.Event.NONE,guard.poll(25_000));
        guard.output(false,30_000);
        assertEquals(PlaybackIdleGuard.Event.STOP_OUTPUT,guard.poll(50_000));
    }
    @Test public void duplicateOutputNotificationsDoNotExtendTheGrace() {
        PlaybackIdleGuard guard=new PlaybackIdleGuard(0);
        guard.playback(true,0);guard.output(false,0);guard.output(false,10_000);
        assertEquals(PlaybackIdleGuard.Event.STOP_OUTPUT,guard.poll(20_000));
    }
    @Test public void idleAndOutputPoliciesCanBeConfiguredIndependently() {
        PlaybackIdleGuard guard=new PlaybackIdleGuard(0);
        guard.configure(0,60,true,20);guard.playback(true,0);
        assertEquals(PlaybackIdleGuard.Event.NONE,guard.poll(100*HOUR));
        guard.output(false,100*HOUR);
        assertEquals(PlaybackIdleGuard.Event.STOP_OUTPUT,guard.poll(100*HOUR+20_000));
    }
    @Test public void disabledOutputPolicyDoesNotInterruptPlayback() {
        PlaybackIdleGuard guard=new PlaybackIdleGuard(0);
        guard.configure(240,60,false,20);guard.playback(true,0);guard.output(false,0);
        assertEquals(PlaybackIdleGuard.Event.NONE,guard.poll(HOUR));
    }
    @Test public void stoppingPlaybackCancelsAutomaticChecks() {
        PlaybackIdleGuard guard=new PlaybackIdleGuard(0);
        guard.playback(true,0);guard.playback(false,HOUR);
        assertEquals(PlaybackIdleGuard.Event.NONE,guard.poll(24*HOUR));
    }

    @Test public void disablingIdleDuringWarningDoesNotStopPlayback() {
        PlaybackIdleGuard guard=new PlaybackIdleGuard(0);guard.playback(true,0);
        assertEquals(PlaybackIdleGuard.Event.WARN,guard.poll(4*HOUR));
        guard.configure(0,60,false,20);
        assertEquals(PlaybackIdleGuard.Event.NONE,guard.poll(5*HOUR));
    }
    @Test public void restartAfterStopGetsNewWindowAndOnlyOneStop() {
        PlaybackIdleGuard guard=new PlaybackIdleGuard(0);guard.playback(true,0);
        assertEquals(PlaybackIdleGuard.Event.STOP_IDLE,guard.poll(5*HOUR));
        guard.playback(false,5*HOUR);guard.playback(true,6*HOUR);
        assertEquals(PlaybackIdleGuard.Event.NONE,guard.poll(6*HOUR));
        assertEquals(PlaybackIdleGuard.Event.STOP_IDLE,guard.poll(11*HOUR));
        assertEquals(PlaybackIdleGuard.Event.NONE,guard.poll(12*HOUR));
    }
    @Test public void outputRecoveryAfterStopDoesNotGenerateAnotherStop() {
        PlaybackIdleGuard guard=new PlaybackIdleGuard(0);guard.playback(true,0);guard.output(false,0);
        assertEquals(PlaybackIdleGuard.Event.STOP_OUTPUT,guard.poll(20000));
        guard.output(true,21000);guard.interaction(22000);
        assertEquals(PlaybackIdleGuard.Event.NONE,guard.poll(10*HOUR));
    }
}
