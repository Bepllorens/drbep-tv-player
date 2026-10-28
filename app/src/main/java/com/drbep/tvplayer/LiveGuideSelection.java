package com.drbep.tvplayer;
import java.util.*;
/** Session-only selection, bounded by day/source identity; never stores playback data. */
final class LiveGuideSelection {
 private final LinkedHashMap<String,String> selected=new LinkedHashMap<String,String>(16,.75f,true){
  protected boolean removeEldestEntry(Map.Entry<String,String> e){return size()>12;}
 };
 int resolve(String window,List<String> ids,int preferred){
  if(ids.isEmpty())return -1;
  int i=ids.indexOf(selected.get(window));
  if(i<0)i=Math.max(0,Math.min(preferred,ids.size()-1));
  selected.put(window,ids.get(i));return i;
 }
 int move(String window,List<String> ids,int preferred,int delta){
  int i=resolve(window,ids,preferred);if(i<0)return -1;
  i=Math.max(0,Math.min(i+delta,ids.size()-1));selected.put(window,ids.get(i));return i;
 }
 void select(String window,String id){selected.put(window,id);}
}
