package com.drbep.tvplayer;
import org.junit.Test;import static org.junit.Assert.*;
public class LiveProgrammeSnapshotTest {
 private StartupHomeHubUiModel.ContinueCard card(String subtitle,Runnable action){
  return new StartupHomeHubUiModel.ContinueCard("Channel",subtitle,"logo","Channel",false,0f,action);
 }
 @Test public void snapshotResolutionIsLazyAndDoesNotInvokePlaybackOrMutateOtherRows(){
  int[] reads={0},plays={0};Runnable play=()->plays[0]++;
  StartupHomeHubUiModel.ContinueCard visible=card("old",play),hidden=card("hidden",play);
  StartupHomeHubUiModel.ContinueCard fresh=card("new title",play);fresh.schedule="10:00 – 11:00";fresh.description="Synopsis";
  visible.initialProgramme=()->{reads[0]++;return fresh;};
  hidden.initialProgramme=()->{throw new AssertionError("offscreen snapshot read");};
  assertEquals(0,reads[0]);
  StartupHomeHubUiModel.ContinueCard resolved=visible.resolveProgrammeSnapshot();
  assertEquals("new title",resolved.subtitle);assertEquals("Synopsis",resolved.description);
  assertEquals("10:00 – 11:00",resolved.schedule);assertEquals("old",visible.subtitle);
  assertEquals(1,reads[0]);assertEquals(0,plays[0]);resolved.onClick.run();assertEquals(1,plays[0]);
 }
 @Test public void eagerCardsAndMissingSnapshotKeepTheirExistingData(){
  StartupHomeHubUiModel.ContinueCard c=card("Programme",null);assertSame(c,c.resolveProgrammeSnapshot());
  c.initialProgramme=()->null;assertSame(c,c.resolveProgrammeSnapshot());
 }
}
