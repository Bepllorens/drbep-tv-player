package com.drbep.tvplayer;
import org.junit.Test;import static org.junit.Assert.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import org.json.JSONObject;import com.drbep.tvplayer.playlists.XmltvStreamReader;
public class PlaylistGuideTest {
 @Test public void xmltvUsesTimezoneAndKeepsChannelId()throws Exception {
  List<XmltvStreamReader.Programme> entries=new ArrayList<>();new XmltvStreamReader().read(new ByteArrayInputStream("<tv><programme channel=\"channel-1\" start=\"20261008080000 +0200\" stop=\"20261008090000 +0200\"><title>Programa &amp; título</title><desc>Texto</desc></programme></tv>".getBytes(StandardCharsets.UTF_8)),entries::add);
  assertEquals(1,entries.size());assertEquals("channel-1",entries.get(0).channel);assertEquals(3600000,entries.get(0).end-entries.get(0).start);assertEquals("Programa & título",entries.get(0).title);
 }
 @Test public void rejectsXmlEntities()throws Exception{try{new XmltvStreamReader().read(new ByteArrayInputStream("<!DOCTYPE tv [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><tv>&x;</tv>".getBytes(StandardCharsets.UTF_8)),p->{});fail();}catch(org.xml.sax.SAXException | IOException expected){}}
 @Test public void catchupWindowAndTemplate()throws Exception{
  long now=1800000000000L,start=now-3600000,end=start+1800000;JSONObject a=new JSONObject().put("catchup-days",7).put("catchup","default").put("catchup-source","https://example.test/archive?start={utc}&duration={duration}&now={lutc}");
  assertEquals("https://example.test/archive?start="+start/1000+"&duration=1800&now="+now/1000,PlaylistCatchup.url("https://example.test/live",a,start,end,now));
  try{PlaylistCatchup.url("https://example.test/live",a,now-9*86400000L,end,now);fail();}catch(IllegalArgumentException expected){}
 }
}
