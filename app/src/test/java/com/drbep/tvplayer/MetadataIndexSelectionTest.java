package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class MetadataIndexSelectionTest {
 @Test public void selectionUsesBoundParametersIncludingUntrustedNames(){
  MetadataIndexSelection s=new MetadataIndexSelection("a' OR 1=1 --","series");
  assertEquals("(?='' OR source=?) AND (?='' OR kind=?)",s.where());
  assertArrayEquals(new String[]{s.source,s.source,"series","series"},s.args());
  assertFalse(s.where().contains(s.source));
 }
 @Test public void emptySelectionRetainsAllCatalogCompatibility(){
  assertArrayEquals(new String[]{"","","",""},new MetadataIndexSelection("","").args());
  assertEquals("movie",new MetadataIndexSelection("a","movie").kind);
 }
 @Test public void rejectsUnsupportedKindsAndOversizedIdentifiers(){
  for(String kind:new String[]{"tv","all","live"})try{new MetadataIndexSelection("",kind);fail();}catch(IllegalArgumentException expected){}
  try{new MetadataIndexSelection(new String(new char[161]),"");fail();}catch(IllegalArgumentException expected){}
 }

 @Test public void groupRequiresSourceAndRemainsBoundParameter(){
  MetadataIndexSelection s=new MetadataIndexSelection("a","series","Drama's");
  assertEquals(5,s.args().length);assertEquals("Drama's",s.args()[4]);
  assertTrue(s.where().contains("g.series=metadata_index.series"));
  assertFalse(s.where().contains("Drama's"));
  try{new MetadataIndexSelection("","series","Drama");fail();}catch(IllegalArgumentException expected){}
  try{MetadataIndexSelection.groupPredicate("untrusted");fail();}catch(IllegalArgumentException expected){}
 }
}
