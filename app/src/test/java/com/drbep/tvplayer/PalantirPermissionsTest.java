package com.drbep.tvplayer;
import org.junit.Test;
import org.json.JSONObject;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

public class PalantirPermissionsTest {
 @Test public void legacyAndMissingPermissionsNeverGrantPalantir() throws Exception {
  OfflinePermissions p=CatalogRepository.parseOfflinePermissions(new JSONObject().put("permissions",new JSONObject().put("vod",true).put("admin",true)));
  assertFalse(p.allowsPalantir());
  assertFalse(new OfflinePermissions().allowsPalantir());
 }
 @Test public void grantRequiresGeneralVodAndAnExplicitPalantirFlag() throws Exception {
  for(boolean vod:new boolean[]{false,true})for(boolean grant:new boolean[]{false,true}){
   OfflinePermissions p=CatalogRepository.parseOfflinePermissions(new JSONObject().put("permissions",new JSONObject().put("vod",vod).put("palantir_vod",grant)));
   assertEquals(vod&&grant,p.allowsPalantir());
  }
 }
 @Test public void binaryRoundTripPreservesGrantAndLegacyVersionDefaultsToDenied() throws Exception {
  OfflinePermissions original=new OfflinePermissions();original.vodEnabled=true;original.palantirEnabled=true;
  original.allowedLiveGroups.add("deportes");original.protectedFilterKeys.add("vod:palantir");
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();CatalogSnapshotStore.writeOfflinePermissions(new DataOutputStream(bytes),original);
  DataInputStream current=new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
  OfflinePermissions decoded=CatalogSnapshotStore.readOfflinePermissions(current,16);
  assertTrue(decoded.allowsPalantir());assertEquals(-1,current.read());
  byte[] legacy=Arrays.copyOf(bytes.toByteArray(),bytes.size()-1);
  OfflinePermissions previous=CatalogSnapshotStore.readOfflinePermissions(new DataInputStream(new ByteArrayInputStream(legacy)),15);
  assertFalse(previous.allowsPalantir());assertEquals(original.allowedLiveGroups,previous.allowedLiveGroups);
  assertEquals(original.protectedFilterKeys,previous.protectedFilterKeys);
 }
 @Test public void railOnlyOffersPalantirWhenItIsAvailableAndKeepsOldPositions() throws Exception {
  MainRailLayout layout=MainRailLayout.decode("{\"version\":1,\"order\":[\"home\",\"movies\",\"series\"],\"hidden\":[]}");
  assertFalse(layout.visibleOrder(Arrays.asList("home","movies","series")).contains("palantir"));
  assertTrue(layout.visibleOrder(Arrays.asList("home","movies","series","palantir")).contains("palantir"));
  assertEquals("home",layout.order.get(0));
 }
}
