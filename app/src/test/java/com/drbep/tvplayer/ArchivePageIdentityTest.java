package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class ArchivePageIdentityTest {
 @Test public void samePageSurvivesBackgroundRefresh() {
  assertEquals(ArchivePageIdentity.key("2026-10-19","","","",false,0),
   ArchivePageIdentity.key("2026-10-19","","","",false,0));
 }
 @Test public void punctuationCannotConfuseDifferentFilters() {
  assertNotEquals(ArchivePageIdentity.key("day","a, b","c","",false,0),
   ArchivePageIdentity.key("day","a","b, c","",false,0));
 }
 @Test public void everyFilterAndPageChangesIdentity() {
  String base=ArchivePageIdentity.key("day","","","",false,0);
  assertNotEquals(base,ArchivePageIdentity.key("other","","","",false,0));
  assertNotEquals(base,ArchivePageIdentity.key("day","channel","","",false,0));
  assertNotEquals(base,ArchivePageIdentity.key("day","","genre","",false,0));
  assertNotEquals(base,ArchivePageIdentity.key("day","","","search",false,0));
  assertNotEquals(base,ArchivePageIdentity.key("day","","","",true,0));
  assertNotEquals(base,ArchivePageIdentity.key("day","","","",false,60));
 }
}
