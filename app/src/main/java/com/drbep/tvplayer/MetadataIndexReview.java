package com.drbep.tvplayer;
import java.util.Arrays;
/** Read-only, keyset-paged review; parameters never become SQL text. */
final class MetadataIndexReview {
 static void validate(String state,long after) {
  if(!("error".equals(state)||"unmatched".equals(state))||after<0)throw new IllegalArgumentException();
 }
 static final String PREFIX="SELECT e.*,s.name AS source_name,m.kind AS index_kind,m.review_row FROM (SELECT rowid AS review_row,* FROM metadata_index WHERE ";
 static final String SUFFIX=") m JOIN sources s ON s.id=m.source AND COALESCE(s.active,s.preview)=m.generation JOIN entries e ON e.source=m.source AND e.generation=m.generation AND e.identity=m.identity AND e.occurrence=m.occurrence LEFT JOIN preferences p ON p.source=e.source AND p.identity=e.identity AND p.occurrence=e.occurrence WHERE m.state=? AND m.review_row>? AND COALESCE(p.hidden,0)=0 ORDER BY m.review_row LIMIT 51";
 static String sql(MetadataIndexSelection selection){return PREFIX+selection.where()+SUFFIX;}
 static String[] args(MetadataIndexSelection selection,String state,long after) {
  validate(state,after);String[] base=selection.args(),args=Arrays.copyOf(base,base.length+2);
  args[base.length]=state;args[base.length+1]=Long.toString(after);return args;
 }
}
