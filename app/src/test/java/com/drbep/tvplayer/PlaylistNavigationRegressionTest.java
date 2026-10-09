package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class PlaylistNavigationRegressionTest {
 @Test public void favoritesAndHistoryKeepTheLiveDrmItemAndItsHeaders(){
  ChannelItem channel=new ChannelItem("drm-live","Canal","tvg","","grupo","https://stream.test/live.mpd","",0,0,false,false,1,"Proveedor",Collections.emptyList(),"widevine","https://license.test","m3u",true);
  channel.playlistStreamHeaders=Collections.singletonMap("Referer","https://origin.test");
  channel.playlistLicenseHeaders=Collections.singletonMap("Authorization","test-placeholder");
  for(String section:Arrays.asList("Favoritos","Historial")) {
   ChannelItem[] chosen={null};
   QuickChannelListUiModel model=QuickChannelListUiFactory.build(section,"",Collections.singletonList(channel),new QuickChannelListUiFactory.Host(){
    public String text(int id){return "Directo";}public String displayName(ChannelItem c){return c.name;}
    public String vodMeta(ChannelItem c){return "VOD";}public String membershipLabel(ChannelItem c,int n){return "";}
    public String protectedTitle(ChannelItem c,String t){return t;}public String protectedMeta(ChannelItem c,String t){return t;}
    public String protectedTypeBadge(ChannelItem c,String t){return t;}public boolean isPlaying(ChannelItem c){return false;}
    public void choose(ChannelItem c){chosen[0]=c;}
   });
   assertFalse(model.items.get(0).vod);model.items.get(0).onClick.run();assertSame(channel,chosen[0]);
   assertEquals("widevine",chosen[0].drmScheme);assertEquals("https://license.test",chosen[0].drmLicenseUrl);
   assertEquals("test-placeholder",chosen[0].playlistLicenseHeaders.get("Authorization"));assertFalse(chosen[0].isVod);
  }
 }
 @Test public void televisionAndOnDemandNeverShareContinueCarousel(){
  StartupHomeHubUiModel.ContinueCard live=new StartupHomeHubUiModel.ContinueCard("Live","","","",false,true,0f,null);
  StartupHomeHubUiModel.ContinueCard movie=new StartupHomeHubUiModel.ContinueCard("Movie","","","",true,false,0.4f,null);
  List<StartupHomeHubUiModel.ContinueCard> mixed=Arrays.asList(live,null,movie);
  assertEquals(Collections.singletonList(live),StartupHomeHubUiModel.continueGroup(mixed,true));
  assertEquals(Collections.singletonList(movie),StartupHomeHubUiModel.continueGroup(mixed,false));
 }
}
