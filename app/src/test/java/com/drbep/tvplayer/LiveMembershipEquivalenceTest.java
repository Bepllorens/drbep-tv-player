package com.drbep.tvplayer;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class LiveMembershipEquivalenceTest {
 static ChannelItem item(String group,List<String> custom,boolean vod,int platform) {
  return new ChannelItem("test","Test","","",group,"","",0,0,vod,false,platform,"Platform",custom,"","","",false);
 }
 static boolean oldMatch(ChannelItem c,String key) {
  if("ALL".equals(key))return true;if(key==null)return false;
  if(key.startsWith("live:"))return ("live:"+c.platformId).equals(key);
  if(!key.startsWith("group:"))return false;
  for(String g:LiveHomePolicy.groups(c))if(("group:"+LiveHomePolicy.normalized(g)).equals(key))return true;
  return false;
 }
 static boolean oldVisible(ChannelItem c,OfflinePermissions p) {
  if(!LiveHomePolicy.isLiveChannel(c)||p==null||!p.liveEnabled)return false;
  if(!p.admin)return true;if(LiveHomePolicy.platformAllowed(p,c.platformId))return true;
  for(String g:LiveHomePolicy.groups(c))if(LiveHomePolicy.groupAllowed(p,g))return true;
  return false;
 }
 @Test public void groupMembershipMatchesReferenceForDuplicatesBlanksAndUnicode() {
  String[] groups={null,""," ","Deportes"," deportes ","Ñandú","İ","group:news"};
  List<List<String>> customs=Arrays.asList(Collections.emptyList(),Arrays.asList("Deportes","Deportes"," deportes "),Arrays.asList("",null,"Ñandú","İ"));
  String[] keys={null,"ALL","live:42","live:4","live:042","group:","group:deportes","group:Ñandú","group:ñandú","group:i","invalid"};
  for(String g:groups)for(List<String> cs:customs)for(String key:keys)
   assertEquals(oldMatch(item(g,cs,false,42),key),LiveHomePolicy.matches(item(g,cs,false,42),key));
 }
 @Test public void permissionCombinationsMatchReference() {
  for(int mask=0;mask<64;mask++) {
   OfflinePermissions p=new OfflinePermissions();p.liveEnabled=(mask&1)!=0;p.admin=(mask&2)!=0;p.liveAccessMetadataKnown=(mask&4)!=0;
   if((mask&8)!=0)p.allowedPlatformIds.add(42);
   if((mask&16)!=0)p.allowedLiveGroups.add("deportes");
   if((mask&32)!=0)p.allowedLiveGroups.add("");
   for(boolean vod:new boolean[]{false,true})for(int id:new int[]{42,43})
    for(String g:new String[]{null,"","Deportes","Otro"}) {
     ChannelItem c=item(g,Arrays.asList("Otro",null,"Deportes","Deportes"),vod,id);
     assertEquals(oldVisible(c,p),LiveHomePolicy.visibleInHome(c,p));
    }
  }
  assertFalse(LiveHomePolicy.visibleInHome(null,new OfflinePermissions()));
  assertFalse(LiveHomePolicy.visibleInHome(item("",Collections.emptyList(),false,42),null));
 }
 @Test public void changedGrantsTakeEffectWithoutStoredMembership() {
  ChannelItem c=item("Deportes",Collections.emptyList(),false,42);
  OfflinePermissions p=new OfflinePermissions();p.admin=true;p.liveEnabled=true;p.liveAccessMetadataKnown=true;
  assertFalse(LiveHomePolicy.visibleInHome(c,p));p.allowedLiveGroups.add("deportes");
  assertTrue(LiveHomePolicy.visibleInHome(c,p));p.allowedLiveGroups.clear();
  assertFalse(LiveHomePolicy.visibleInHome(c,p));
 }
}

