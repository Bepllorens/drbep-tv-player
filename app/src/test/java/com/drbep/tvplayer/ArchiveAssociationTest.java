package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Collections;
public class ArchiveAssociationTest {
 private ChannelItem channel(String name,String epg,String url){return new ChannelItem("1105312",name,epg,"","",url,"",0,0,false,false,1,"Movistar ISM",Collections.emptyList(),"","","",false);}
 @Test public void epgCorrectionInvalidatesEvenWhenDisplayNameDoesNotChange(){
  ChannelItem before=channel("SciFi","wrong-epg","https://example/live");
  ChannelItem after=channel("SciFi","SCI-FI","https://example/live");
  assertTrue(ArchiveAvailability.associationChanged(after,ArchiveAvailability.identityFingerprint(before),"SciFi","wrong-epg"));
 }
 @Test public void unchangedChannelAndRotatingCredentialsKeepCache(){
  ChannelItem before=channel("La 1","TVE","https://example/4521/vxfmt=dp/Manifest.mpd?token=old");
  ChannelItem after=channel("La 1","TVE","https://example/4521/vxfmt=dp/Manifest.mpd?token=new");
  assertFalse(ArchiveAvailability.associationChanged(after,ArchiveAvailability.identityFingerprint(before),"LA 1","TVE"));
  assertFalse(ArchiveAvailability.associationChanged(channel("Vamos","VAMOS",""),null,"Vamos","VAMOS"));
 }
 @Test public void migrationAcceptsOfficialCodeDespiteSpellingDifference(){
  assertFalse(ArchiveAvailability.associationChanged(channel("SYFY","SCI-FI",""),null,"SciFi","SCI-FI"));
  assertTrue(ArchiveAvailability.associationChanged(channel("SciFi","SCI-FI",""),null,"Otro canal","OTHER"));
 }
}
