package com.drbep.tvplayer;
import java.util.List;
/** Remembers programme identity, not its shifting position in the guide. */
final class ArchiveStripSelection {
 private String channelId="",programId="";
 int resolve(List<U7dCatalogEntry> entries) {
  if(entries.isEmpty()) return 0;
  if(entries.get(0).channelId.equals(channelId)) for(int i=0;i<entries.size();i++) if(entries.get(i).id.equals(programId)) return i;
  return entries.size()-1;
 }
 void remember(List<U7dCatalogEntry> entries,int index) {
  if(entries.isEmpty())return;
  U7dCatalogEntry e=entries.get(Math.max(0,Math.min(index,entries.size()-1)));
  channelId=e.channelId;programId=e.id;
 }
}
