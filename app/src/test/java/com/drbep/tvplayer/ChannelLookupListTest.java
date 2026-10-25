package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class ChannelLookupListTest {
 static ChannelItem item(String id) {
  return new ChannelItem(id,id,"","","","","",0,0,false,false,1,"Test",Collections.emptyList(),"","","",false);
 }
 @Test public void matchesLinearLookupWithDuplicatesNullsAndMisses() {
  ChannelLookupList list=new ChannelLookupList();ChannelItem first=item("same"),second=item("same");
  list.addAll(Arrays.asList(null,first,item("other"),second));
  for(int i=0;i<3;i++){assertSame(first,list.find("same"));assertNull(list.find("absent"));assertNull(list.find(null));assertNull(list.find("  "));}
  list.remove(first);assertSame(second,list.find("same"));
 }
 @Test public void missesAreInvalidatedByAppendAndReplacementOfSameSize() {
  ChannelLookupList list=new ChannelLookupList();assertNull(list.find("new"));
  ChannelItem old=item("new"),replacement=item("new");list.add(old);assertSame(old,list.find("new"));
  list.set(0,replacement);assertSame(replacement,list.find("new"));
  list.clear();assertNull(list.find("new"));
 }
 @Test public void iteratorSublistAndBulkMutationsCannotLeaveStaleHits() {
  ChannelLookupList list=new ChannelLookupList();ChannelItem a=item("a"),b=item("b"),c=item("c");
  list.addAll(Arrays.asList(a,b,c));assertSame(b,list.find("b"));
  list.removeIf(x->x==b);assertNull(list.find("b"));
  assertSame(c,list.find("c"));list.subList(1,2).clear();assertNull(list.find("c"));
  ListIterator<ChannelItem> it=list.listIterator();it.next();it.set(b);assertNull(list.find("a"));assertSame(b,list.find("b"));
  it.add(c);assertSame(c,list.find("c"));
  list.replaceAll(x->a);assertNull(list.find("c"));assertSame(a,list.find("a"));
 }
 @Test public void evictionDoesNotChangeResultsOrFirstOccurrenceAfterSort() {
  ChannelLookupList list=new ChannelLookupList();for(int i=0;i<300;i++)list.add(item("id"+i));
  for(int n=0;n<2;n++)for(int i=0;i<300;i++)assertSame(list.get(i),list.find("id"+i));
  ChannelItem original=list.get(5),duplicate=item("id5");list.add(0,duplicate);
  assertSame(duplicate,list.find("id5"));Collections.reverse(list);assertSame(original,list.find("id5"));
 }
}
