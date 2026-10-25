package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class MetadataIndexGroupsTest {
 @Test public void boundedProbeRetainsLegacyForManyGroups(){
  assertTrue(MetadataIndexGroups.grouped(0));assertTrue(MetadataIndexGroups.grouped(32));
  assertFalse(MetadataIndexGroups.grouped(33));
  assertTrue(MetadataIndexGroups.probeSql("").endsWith("LIMIT 33"));
  for(String kind:new String[]{"","movie","series"})assertEquals(MetadataIndexGroups.LEGACY,MetadataIndexGroups.sql(kind,false));
 }
 @Test public void untrustedSourceRemainsBound(){
  String source="x' OR 1=1 --";
  for(String kind:new String[]{"","movie","series"})for(boolean grouped:new boolean[]{false,true}){
   assertFalse(MetadataIndexGroups.sql(kind,grouped).contains(source));
   assertEquals(source,MetadataIndexGroups.args(source,kind,grouped)[0]);
  }
 }
 @Test public void placeholdersMatchBothPaths(){
  for(String kind:new String[]{"","movie","series"}) {
   assertEquals(MetadataIndexGroups.probeArgs("s",kind).length,MetadataIndexGroups.probeSql(kind).chars().filter(c->c=='?').count());
   for(boolean grouped:new boolean[]{false,true})assertEquals(MetadataIndexGroups.args("s",kind,grouped).length,MetadataIndexGroups.sql(kind,grouped).chars().filter(c->c=='?').count());
  }
 }
 @Test public void invalidKindsCannotBecomeSql(){
  for(String kind:new String[]{"tv","movie' OR 1=1 --"})try{MetadataIndexGroups.probeSql(kind);fail();}catch(IllegalArgumentException expected){}
 }
}
