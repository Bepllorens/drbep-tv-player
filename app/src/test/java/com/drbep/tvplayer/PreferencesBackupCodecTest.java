package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class PreferencesBackupCodecTest {
 @Test public void preservesAutomaticTimeout() throws Exception {
  Map<String,Object> m=new HashMap<>();m.put("menu_hide_seconds",0);
  assertEquals(0,PreferencesBackupCodec.decode(PreferencesBackupCodec.encode(m)).get("menu_hide_seconds"));
 }

 @Test public void exportOmitsSecretsAndRoundTrips() throws Exception {
  Map<String,Object> m=new HashMap<>();m.put("ui_palette","graphite");m.put("menu_width_percent",85);
  m.put("token","secret");m.put("parental_pin","1234");m.put("server_url","private");
  m.put("settings_pinned_ids",new HashSet<>(Arrays.asList("temporizador","subtitulos")));
  String json=PreferencesBackupCodec.encode(m);assertFalse(json.contains("secret"));assertFalse(json.contains("1234"));assertFalse(json.contains("private"));
  Map<String,Object> r=PreferencesBackupCodec.decode(json);assertEquals(3,r.size());assertEquals(85,r.get("menu_width_percent"));assertEquals(m.get("settings_pinned_ids"),r.get("settings_pinned_ids"));
 }
 @Test(expected=IllegalArgumentException.class) public void rejectsUnknownKey() throws Exception {PreferencesBackupCodec.decode("{\"format\":\"drbep.preferences\",\"version\":1,\"settings\":{\"parental_pin\":\"1234\"}}");}
 @Test(expected=IllegalArgumentException.class) public void rejectsInvalidWidth() throws Exception {PreferencesBackupCodec.decode("{\"format\":\"drbep.preferences\",\"version\":1,\"settings\":{\"menu_width_percent\":999}}");}
 @Test(expected=IllegalArgumentException.class) public void rejectsFutureVersion() throws Exception {PreferencesBackupCodec.decode("{\"format\":\"drbep.preferences\",\"version\":2,\"settings\":{\"ui_palette\":\"aurora\"}}");}
 @Test(expected=IllegalArgumentException.class) public void rejectsWrongType() throws Exception {PreferencesBackupCodec.decode("{\"format\":\"drbep.preferences\",\"version\":1,\"settings\":{\"startup_hub_disabled\":\"true\"}}");}
 @Test(expected=IllegalArgumentException.class) public void rejectsOversize() throws Exception {PreferencesBackupCodec.decode(new String(new char[65537]));}
 @Test public void searchIgnoresAccentsAndCaseAndFindsEveryWord() {assertTrue(SettingsSearchIndex.matches("SUBTITULOS tamaño","Subtítulos y tamaño del texto"));assertFalse(SettingsSearchIndex.matches("subtitulos calidad","Subtítulos"));assertEquals("colores-y-contraste",SettingsSearchIndex.key("Colores y contraste"));}
}
