package com.drbep.tvplayer;
import org.junit.Test;
import java.util.*;
import java.util.function.Predicate;
import static org.junit.Assert.*;
public class FavoriteQuickChannelsTest {
 static ChannelItem item(String id,String name){return new ChannelItem(id,name,"","","","","",0,0,false,false,42,"Platform",Collections.emptyList(),"","","",false);}
 static List<ChannelItem> reference(List<ChannelItem> channels,Set<String> favorites,List<String> order,Predicate<ChannelItem> hidden){
  List<ChannelItem> result=new ArrayList<>();if(favorites.isEmpty())return result;
  Map<String,ChannelItem> byId=new LinkedHashMap<>();
  for(ChannelItem c:channels)if(c!=null&&c.id!=null&&!c.id.trim().isEmpty()&&!byId.containsKey(c.id))byId.put(c.id,c);
  Set<String> added=new HashSet<>();
  for(String id:order){ChannelItem c=byId.get(id);if(c!=null&&!hidden.test(c)&&favorites.contains(id)&&added.add(id))result.add(c);}
  for(ChannelItem c:channels)if(c!=null&&c.id!=null&&!hidden.test(c)&&favorites.contains(c.id)&&added.add(c.id))result.add(c);
  return result;
 }
 @Test public void randomizedCataloguesPreserveExactObjectsAndOrder(){
  Random random=new Random(3001026);
  for(int run=0;run<300;run++){
   List<ChannelItem> channels=new ArrayList<>();
   for(int i=0;i<150;i++)channels.add(i%17==0?null:item("id"+random.nextInt(50),i%7==0?"hidden":"visible"));
   channels.add(item("","visible"));
   Set<String> favorites=new HashSet<>();
   for(int i=0;i<20;i++)favorites.add("id"+random.nextInt(70));
   if(run%3==0)favorites.add("");
   if(run%11==0)favorites.clear();
   List<String> order=new ArrayList<>();
   for(int i=0;i<25;i++)order.add("id"+random.nextInt(70));
   if(run%4==0)order.clear();
   order.add(null);order.add("");
   Predicate<ChannelItem> hidden=c->c.name.equals("hidden");
   assertEquals(reference(channels,favorites,order,hidden),FavoriteQuickChannels.build(channels,favorites,order,hidden));
  }
 }
 @Test public void hiddenFirstDuplicateDoesNotSuppressVisibleFallback(){
  ChannelItem first=item("a","hidden"),second=item("a","visible"),other=item("b","visible");
  assertEquals(Arrays.asList(other,second),FavoriteQuickChannels.build(Arrays.asList(first,second,other),new HashSet<>(Arrays.asList("a","b")),Arrays.asList("a","b"),c->c==first));
 }
 @Test public void changedProtectionAndRemovedFavoritesApplyImmediately(){
  ChannelItem c=item("a","visible");Set<String> ids=new HashSet<>(Arrays.asList("a"));
  assertEquals(Arrays.asList(c),FavoriteQuickChannels.build(Arrays.asList(c),ids,Collections.emptyList(),x->false));
  assertTrue(FavoriteQuickChannels.build(Arrays.asList(c),ids,Collections.emptyList(),x->true).isEmpty());
  ids.clear();assertTrue(FavoriteQuickChannels.build(Arrays.asList(c),ids,Collections.emptyList(),x->false).isEmpty());
 }
 @Test public void protectionIsOnlyQueriedForFavorites(){
  List<ChannelItem> channels=new ArrayList<>();for(int i=0;i<4000;i++)channels.add(item("id"+i,"visible"));
  Set<String> ids=new HashSet<>(Arrays.asList("id1","id2"));int[] calls={0};
  List<ChannelItem> result=FavoriteQuickChannels.build(channels,ids,Arrays.asList("id2"),c->{calls[0]++;return false;});
  assertEquals(2,result.size());assertEquals(3,calls[0]);
 }
}
