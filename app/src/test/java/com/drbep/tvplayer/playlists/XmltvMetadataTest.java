package com.drbep.tvplayer.playlists;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONObject;
public class XmltvMetadataTest {
 @Test public void preservesMetadataWithoutRequiringArtwork()throws Exception{
  String xml="<tv><programme channel='c' start='20261012120000 +0200' stop='20261012130000 +0200'><title>Programa</title><desc>Resumen</desc><category>Deportes</category><sub-title>Final</sub-title><date>2026</date><credits><actor>A</actor><actor>B</actor></credits><rating><value>12</value></rating><star-rating><value>8/10</value></star-rating></programme></tv>";
  List<XmltvStreamReader.Programme> rows=new ArrayList<>();new XmltvStreamReader().read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),rows::add);
  JSONObject p=new JSONObject(rows.get(0).metadata());assertEquals("A, B",p.getJSONObject("credits").getString("actor"));assertEquals("12",p.getString("age"));assertEquals("8/10",p.getString("rating"));assertEquals("",p.getString("image"));assertEquals("Resumen",rows.get(0).description);
 }
 @Test public void channelNamesAndEpisodeNumbersArePreserved()throws Exception {
  java.util.Map<String,String> names=new java.util.HashMap<>();
  new XmltvStreamReader().read(new ByteArrayInputStream("<tv><channel id='la1'><display-name>La 1 HD</display-name></channel></tv>".getBytes(StandardCharsets.UTF_8)),p->{},names::put);
  assertEquals("La 1 HD",names.get("la1"));assertEquals("T1 E2",XmltvStreamReader.episode("0.1.","xmltv_ns"));
 }
 @Test public void rejectsExternalEntities()throws Exception{
  try{new XmltvStreamReader().read(new ByteArrayInputStream("<!DOCTYPE tv [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><tv/>".getBytes(StandardCharsets.UTF_8)),p->{});fail("DTD accepted");}catch(org.xml.sax.SAXException|IOException expected){}
 }
}
