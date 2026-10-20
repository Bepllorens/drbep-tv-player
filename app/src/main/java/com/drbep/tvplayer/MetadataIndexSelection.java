package com.drbep.tvplayer;
/** Immutable parameterized scope shared by queue consumption, retry and progress. */
final class MetadataIndexSelection {
 final String source,kind,group;
 MetadataIndexSelection(String source,String kind){this(source,kind,"");}
 MetadataIndexSelection(String source,String kind,String group) {
  if(group==null || group.length()>300 || (!group.isEmpty() && (source==null || source.isEmpty())) || source==null || source.length()>160 || kind==null
    || !(kind.isEmpty() || kind.equals("movie") || kind.equals("series")))
   throw new IllegalArgumentException("Invalid metadata selection");
  this.source=source;this.kind=kind;this.group=group;
 }
 String where(){return "(?='' OR source=?) AND (?='' OR kind=?)"+(group.isEmpty()?"":" AND "+groupPredicate("metadata_index"));}
 String[] args(){return group.isEmpty()?new String[]{source,source,kind,kind}:new String[]{source,source,kind,kind,group};}

 // A series belongs to a group if any visible episode does, regardless of its representative.
 static String groupPredicate(String alias) {
  if(!alias.equals("e") && !alias.equals("metadata_index"))throw new IllegalArgumentException();
  return GROUP_SQL.replace("@",alias);
 }
 static final String GROUP_SQL="EXISTS (SELECT 1 FROM entries g JOIN sources gs ON gs.id=g.source AND COALESCE(gs.active,gs.preview)=g.generation LEFT JOIN preferences gp ON gp.source=g.source AND gp.identity=g.identity AND gp.occurrence=g.occurrence WHERE g.source=@.source AND g.generation=@.generation AND g.kind=@.kind AND g.group_name=? AND COALESCE(gp.hidden,0)=0 AND ((@.kind='series' AND g.series=@.series) OR (@.kind='movie' AND g.identity=@.identity AND g.occurrence=@.occurrence)))";
}
