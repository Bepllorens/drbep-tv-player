package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class MetadataIndexSelectionTest {
 @Test public void selectionUsesBoundParametersIncludingUntrustedNames(){
  MetadataIndexSelection s=new MetadataIndexSelection("a' OR 1=1 --","series");
  assertEquals("source=? AND kind=?",s.where());
  assertArrayEquals(new String[]{s.source,"series"},s.args());
  assertFalse(s.where().contains(s.source));
 }
 @Test public void emptySelectionRetainsAllCatalogCompatibility(){
  assertArrayEquals(new String[]{},new MetadataIndexSelection("","").args());
  assertEquals("movie",new MetadataIndexSelection("a","movie").kind);
 }
 @Test public void rejectsUnsupportedKindsAndOversizedIdentifiers(){
  for(String kind:new String[]{"tv","all","live"})try{new MetadataIndexSelection("",kind);fail();}catch(IllegalArgumentException expected){}
  try{new MetadataIndexSelection(new String(new char[161]),"");fail();}catch(IllegalArgumentException expected){}
 }

 @Test public void groupRequiresSourceAndRemainsBoundParameter(){
  MetadataIndexSelection s=new MetadataIndexSelection("a","series","Drama's");
  assertEquals(3,s.args().length);assertEquals("Drama's",s.args()[2]);
  assertTrue(s.where().contains("g.series=metadata_index.series"));
  assertFalse(s.where().contains("Drama's"));
  try{new MetadataIndexSelection("","series","Drama");fail();}catch(IllegalArgumentException expected){}
  try{MetadataIndexSelection.groupPredicate("untrusted");fail();}catch(IllegalArgumentException expected){}
 }
 @Test public void allScopeCombinationsKeepPlaceholderOrder(){
  String[] sources={"","list"},kinds={"","movie","series"};
  for(String source:sources)for(String kind:kinds){
   MetadataIndexSelection s=new MetadataIndexSelection(source,kind);
   assertEquals(s.args().length,s.where().chars().filter(c->c=='?').count());
   assertFalse(s.where().contains(" OR "));
   if(source.isEmpty()&&kind.isEmpty())assertEquals("1=1",s.where());
   else if(source.isEmpty())assertArrayEquals(new String[]{kind},s.args());
   else if(kind.isEmpty())assertArrayEquals(new String[]{source},s.args());
   else assertArrayEquals(new String[]{source,kind},s.args());
  }
 }
 @Test public void kindOnlyAggregateKeepsScanPlanWithoutChangingScope(){
  MetadataIndexSelection s=new MetadataIndexSelection("","movie");
  assertEquals("(?='' OR kind=?)",s.countWhere());
  assertArrayEquals(new String[]{"movie","movie"},s.countArgs());
  MetadataIndexSelection scoped=new MetadataIndexSelection("a","series","Drama");
  assertEquals(scoped.where(),scoped.countWhere());
  assertArrayEquals(scoped.args(),scoped.countArgs());
 }
 @Test public void specializedGroupKeepsMixedFallbackAndValidatesInputs(){
  assertEquals(MetadataIndexSelection.groupPredicate("metadata_index"),
    MetadataIndexSelection.groupPredicate("metadata_index",""));
  String movie=MetadataIndexSelection.groupPredicate("e","movie");
  String series=MetadataIndexSelection.groupPredicate("e","series");
  assertTrue(movie.contains("g.identity=e.identity AND g.occurrence=e.occurrence"));
  assertTrue(series.contains("g.series=e.series"));
  assertFalse(movie.contains(" OR "));assertFalse(series.contains(" OR "));
  assertTrue(new MetadataIndexSelection("a","","Drama").where().contains(" OR "));
  try{MetadataIndexSelection.groupPredicate("e","bad");fail();}catch(IllegalArgumentException expected){}
  try{MetadataIndexSelection.groupPredicate("bad","movie");fail();}catch(IllegalArgumentException expected){}
 }
}
