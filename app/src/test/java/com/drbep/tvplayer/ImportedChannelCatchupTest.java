package com.drbep.tvplayer;
import org.junit.Test;import static org.junit.Assert.*;import java.time.Instant;import java.util.*;
public class ImportedChannelCatchupTest {
 private ChannelItem channel(){
  ChannelItem c=new ChannelItem("100","Fixture","fixture","","Fixture","https://example.invalid/live.mpd","",0,1,false,false,99,"Fixture",new ArrayList<>(),"clearkey","https://example.invalid/license/100","",true);
  c.importedCatchupType="default";c.importedCatchupDays=30;c.importedCatchupSource="https://example.invalid/archive.mpd?start={start_iso}&end={end_iso}";
  return c;
 }
 @Test public void isoDatesAreUtcAndPreserveWindow()throws Exception{
  long start=Instant.parse("2026-10-25T01:30:00Z").toEpochMilli(),end=start+3600000,now=end+60000;
  TimeZone original=TimeZone.getDefault();try{TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"));
   assertEquals("https://example.invalid/archive.mpd?start=2026-10-25T01:30:00Z&end=2026-10-25T02:30:00Z",ImportedChannelCatchup.url(channel(),start,end,now));
  }finally{TimeZone.setDefault(original);}
 }
 @Test public void replayKeepsOriginalLicenseAndNoProviderFallback()throws Exception{
  long now=1800000000000L;ChannelItem c=channel();
  ChannelItem replay=ImportedChannelCatchup.item(c,"Programme","",now-3600000,now-1800000,now);
  assertEquals(c.drmLicenseUrl,replay.drmLicenseUrl);assertEquals(c.drmScheme,replay.drmScheme);
  assertTrue(replay.directPlayback);assertTrue(replay.isVod);assertTrue(ImportedChannelCatchup.replay(replay));assertEquals("",replay.fallbackPlayUrl);
  assertFalse(ImportedChannelCatchup.available(c,now+1000,now+2000,now));
  assertFalse(ImportedChannelCatchup.available(c,now-31L*86400000,now-30L*86400000,now));
 }
 @Test public void providerRoutesRemainAuthoritative(){
  ChannelItem c=new ChannelItem("1","La 1","","","Movistar","https://example.invalid/live","",0,1,false,false,1,"Movistar",new ArrayList<>(),"","","",true);
  c.importedCatchupType="default";c.importedCatchupDays=30;c.importedCatchupSource="https://example.invalid/{utc}";
  assertFalse(ImportedChannelCatchup.configured(c));
 }
 @Test public void unknownMarkersStillRejected(){
  ChannelItem c=channel();c.importedCatchupSource="https://example.invalid/{unknown}";
  assertFalse(ImportedChannelCatchup.available(c,1000,2000,3000));
 }

 @Test public void replayResolvesSourceIdentityWithoutChangingPlaybackIdentity() {
  PlayerController.PlaybackRequest r=new PlayerController.PlaybackRequest("imported-catchup:100:1800000000000","Programme","Fixture","https://example.invalid/archive.mpd","","auto","","",true,true,"imported_catchup");
  assertEquals("100",PlayerController.streamInfoChannelId(r));
  assertEquals("imported-catchup:100:1800000000000",r.channelId);
  PlayerController.PlaybackRequest live=new PlayerController.PlaybackRequest("100","Channel","Fixture","https://example.invalid/live.mpd","","auto","","",true);
  assertEquals("100",PlayerController.streamInfoChannelId(live));
 }

 @Test public void importedArchiveResolvesBeforePlaybackDespiteVodPresentation()throws Exception{
  ChannelItem c=ImportedChannelCatchup.item(channel(),"Programme","",1000,2000,3000);
  c=new ChannelItem(c.id,c.name,c.tvgId,"","Catchup",c.playUrl,"",0,1,true,false,99,"Fixture",new ArrayList<>(),"clearkey","drbep-secure-stream:100","",true,"","",1,"imported_catchup");
  PlayerController.PlaybackRequest r=new PlayerController.PlaybackRequest(c.id,c.name,c.platformName,c.playUrl,"","auto",c.drmScheme,c.drmLicenseUrl,true,true,c.playbackProfile);
  assertTrue(PlaybackStreamInfoPolicy.shouldResolveBeforePlayback(true,c,r,c.name));
 }
}
