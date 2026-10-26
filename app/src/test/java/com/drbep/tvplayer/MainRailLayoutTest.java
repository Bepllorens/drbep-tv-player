package com.drbep.tvplayer;
import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

public class MainRailLayoutTest {
 @Test public void defaultOrderMatchesExistingMenuAndHomeCannotBeHidden(){
  MainRailLayout layout=MainRailLayout.defaults();
  assertEquals(Arrays.asList("search","home","live","library","movies","series","guide","catchup","palantir"),layout.order);
  for(String id:MainRailLayout.IDS)layout=layout.withVisible(id,false);
  assertEquals(Arrays.asList("home"),layout.visibleOrder(MainRailLayout.IDS));
  assertEquals(Arrays.asList("home"),layout.visibleOrder(Collections.emptyList()));
 }
 @Test public void reorderingAndVisibilitySurviveSerializationWithoutChangingLabelsOrCallbacks() throws Exception {
  MainRailLayout layout=MainRailLayout.defaults().move("movies",-1).move("movies",-1).withVisible("guide",false);
  MainRailLayout restored=MainRailLayout.decode(layout.encode());
  assertEquals(layout.order,restored.order);assertEquals(layout.hidden,restored.hidden);
  assertEquals("movies",restored.order.get(2));
  assertTrue(MainRailLayout.validBackup(layout.encode()));
 }
 @Test public void unavailableFeaturesKeepConfiguredPositionsUntilTheyAreAvailableAgain(){
  MainRailLayout layout=MainRailLayout.defaults().move("series",-1);
  List<String> without=layout.visibleOrder(Arrays.asList("search","home","live","library","guide","catchup"));
  assertFalse(without.contains("series"));assertFalse(without.contains("movies"));
  assertEquals(layout.order,layout.visibleOrder(MainRailLayout.IDS));
  List<String> differentAvailabilityOrder=new ArrayList<>(MainRailLayout.IDS);
  Collections.reverse(differentAvailabilityOrder);
  assertEquals(layout.order,layout.visibleOrder(differentAvailabilityOrder));
 }
 @Test public void malformedOrFutureLocalPreferencesRemainRecoverable() throws Exception {
  assertEquals(MainRailLayout.IDS,MainRailLayout.decode("{").order);
  assertEquals(MainRailLayout.IDS,MainRailLayout.decode("{\"version\":2}").order);
  JSONObject raw=new JSONObject().put("version",1).put("order",new JSONArray(Arrays.asList("series","series","unknown")))
   .put("hidden",new JSONArray(Arrays.asList("home","guide","unknown")));
  MainRailLayout layout=MainRailLayout.decode(raw.toString());
  assertEquals("series",layout.order.get(0));assertEquals(MainRailLayout.IDS.size(),new HashSet<>(layout.order).size());
  assertTrue(layout.visible("home"));assertFalse(layout.visible("guide"));
 }
 @Test public void boundariesAndUnknownIdsDoNotMutateTheLayout(){
  MainRailLayout layout=MainRailLayout.defaults();
  assertSame(layout,layout.move("search",-1));assertSame(layout,layout.move("palantir",1));
  assertSame(layout,layout.move("unknown",1));assertSame(layout,layout.withVisible("settings",false));
  MainRailLayout changed=layout.move("live",-1);
  assertEquals("home",layout.order.get(1));assertEquals("live",changed.order.get(1));
 }
 @Test public void backupRejectsUnknownDataAndProtectedSections() throws Exception {
  JSONObject json=new JSONObject(MainRailLayout.defaults().encode());
  json.put("token","fixture");assertFalse(MainRailLayout.validBackup(json.toString()));json.remove("token");
  json.put("hidden",new JSONArray(Arrays.asList("home")));assertFalse(MainRailLayout.validBackup(json.toString()));
  json.put("hidden",new JSONArray());json.put("order",new JSONArray(Arrays.asList("unknown")));
  assertFalse(MainRailLayout.validBackup(json.toString()));
 }
 @Test public void portableBackupIncludesLayoutAndLegacyCopiesStayCompatible() throws Exception {
  String encoded=MainRailLayout.defaults().withVisible("library",false).encode();
  Map<String,Object> values=new HashMap<>();values.put(MainRailLayout.PREF,encoded);values.put("token","fixture-secret");
  String backup=PreferencesBackupCodec.encode(values);
  assertFalse(backup.contains("fixture-secret"));
  assertEquals(encoded,PreferencesBackupCodec.decode(backup).get(MainRailLayout.PREF));
  assertTrue(PreferencesBackupCodec.decode(PreferencesBackupCodec.encode(Collections.singletonMap("ui_palette","graphite"))).containsKey("ui_palette"));
 }
 @Test public void primaryCardIdentityIsIndependentOfItsDisplayedTitle(){
  StartupHomeHubUiModel.PrimaryCard card=new StartupHomeHubUiModel.PrimaryCard("","Films","","",true,()->{},MainRailLayout.MOVIES);
  assertEquals("Films",card.title);assertEquals("movies",card.navigationId);
 }
 @Test(expected=UnsupportedOperationException.class) public void preferenceOrderCannotBeMutated(){
  MainRailLayout.defaults().order.clear();
 }
}
