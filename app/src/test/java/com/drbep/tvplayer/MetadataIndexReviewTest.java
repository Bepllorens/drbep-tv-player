package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class MetadataIndexReviewTest {
 @Test public void bindsReviewStateAndCursorAfterScope(){
  MetadataIndexSelection selection=new MetadataIndexSelection("a","series","Drama");
  assertArrayEquals(new String[]{"a","series","Drama","error","50"},MetadataIndexReview.args(selection,"error",50));
  String sql=MetadataIndexReview.sql(selection);
  assertTrue(sql.endsWith("ORDER BY m.review_row LIMIT 51"));
  assertTrue(sql.contains("COALESCE(s.active,s.preview)=m.generation"));
  assertTrue(sql.contains("COALESCE(p.hidden,0)=0"));
  assertFalse(sql.contains("Drama"));
 }
 @Test public void disallowsMutationsAndUnsupportedReviewStates(){
  for(String state:new String[]{"done","pending","skipped","delete",""})try{MetadataIndexReview.validate(state,0);fail();}catch(IllegalArgumentException expected){}
  try{MetadataIndexReview.validate("error",-1);fail();}catch(IllegalArgumentException expected){}
  MetadataIndexReview.validate("unmatched",0);
 }
}
