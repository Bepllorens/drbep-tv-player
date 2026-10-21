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
 static final String ALL_SQL="1=1";
 static final String SOURCE_SQL="source=?";
 static final String KIND_SQL="kind=?";
 static final String SOURCE_KIND_SQL="source=? AND kind=?";
 String where(){
  String base=source.isEmpty()?(kind.isEmpty()?ALL_SQL:KIND_SQL):(kind.isEmpty()?SOURCE_SQL:SOURCE_KIND_SQL);
  return base+(group.isEmpty()?"":" AND "+groupPredicate("metadata_index",kind));
 }
 String[] args(){
  java.util.List<String> values=new java.util.ArrayList<>();
  if(!source.isEmpty())values.add(source);
  if(!kind.isEmpty())values.add(kind);
  if(!group.isEmpty())values.add(group);
  return values.toArray(new String[0]);
 }

 // A kind-only count otherwise chooses the primary-key index and fetches many
 // table rows for state. Keep the old predicate for this aggregate only.
 static final String COUNT_KIND_SQL="(?='' OR kind=?)";
 String countWhere(){return source.isEmpty()&&!kind.isEmpty()?COUNT_KIND_SQL:where();}
 String[] countArgs(){return source.isEmpty()&&!kind.isEmpty()?new String[]{kind,kind}:args();}

 // A series belongs to a group if any visible episode does, regardless of its representative.
 static String groupPredicate(String alias) {
  if(!alias.equals("e") && !alias.equals("metadata_index"))throw new IllegalArgumentException();
  return GROUP_SQL.replace("@",alias);
 }
 static String groupPredicate(String alias,String kind) {
  if(!alias.equals("e") && !alias.equals("metadata_index"))throw new IllegalArgumentException();
  String sql;
  if(kind.equals("movie"))sql=GROUP_MOVIE_SQL;
  else if(kind.equals("series"))sql=GROUP_SERIES_SQL;
  else if(kind.isEmpty())sql=GROUP_SQL;
  else throw new IllegalArgumentException();
  return sql.replace("@",alias);
 }
 static final String GROUP_MOVIE_SQL="EXISTS (SELECT 1 FROM entries g JOIN sources gs ON gs.id=g.source AND COALESCE(gs.active,gs.preview)=g.generation LEFT JOIN preferences gp ON gp.source=g.source AND gp.identity=g.identity AND gp.occurrence=g.occurrence WHERE g.source=@.source AND g.generation=@.generation AND g.kind=@.kind AND g.group_name=? AND COALESCE(gp.hidden,0)=0 AND g.identity=@.identity AND g.occurrence=@.occurrence)";
 static final String GROUP_SERIES_SQL="EXISTS (SELECT 1 FROM entries g JOIN sources gs ON gs.id=g.source AND COALESCE(gs.active,gs.preview)=g.generation LEFT JOIN preferences gp ON gp.source=g.source AND gp.identity=g.identity AND gp.occurrence=g.occurrence WHERE g.source=@.source AND g.generation=@.generation AND g.kind=@.kind AND g.group_name=? AND COALESCE(gp.hidden,0)=0 AND g.series=@.series)";
 static final String GROUP_SQL="EXISTS (SELECT 1 FROM entries g JOIN sources gs ON gs.id=g.source AND COALESCE(gs.active,gs.preview)=g.generation LEFT JOIN preferences gp ON gp.source=g.source AND gp.identity=g.identity AND gp.occurrence=g.occurrence WHERE g.source=@.source AND g.generation=@.generation AND g.kind=@.kind AND g.group_name=? AND COALESCE(gp.hidden,0)=0 AND ((@.kind='series' AND g.series=@.series) OR (@.kind='movie' AND g.identity=@.identity AND g.occurrence=@.occurrence)))";
}
