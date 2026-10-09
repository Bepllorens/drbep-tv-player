package com.drbep.tvplayer;
import org.junit.Test;import java.util.*;import static org.junit.Assert.*;
public class ArchiveStripSelectionTest {
 private U7dCatalogEntry e(String channel,String id){return new U7dCatalogEntry(id,channel,channel,id,"","","",1,2,false,null,null,null,null);}
 @Test public void firstEntryIsLatest(){ArchiveStripSelection s=new ArchiveStripSelection();assertEquals(2,s.resolve(Arrays.asList(e("a","old"),e("a","middle"),e("a","latest"))));}
 @Test public void refreshKeepsProgrammeInsteadOfIndex(){ArchiveStripSelection s=new ArchiveStripSelection();s.remember(Arrays.asList(e("a","old"),e("a","chosen")),1);assertEquals(0,s.resolve(Arrays.asList(e("a","chosen"),e("a","new"))));}
 @Test public void differentChannelAndExpiredSelectionUseLatest(){ArchiveStripSelection s=new ArchiveStripSelection();s.remember(Arrays.asList(e("a","old")),0);assertEquals(1,s.resolve(Arrays.asList(e("b","x"),e("b","y"))));assertEquals(1,s.resolve(Arrays.asList(e("a","new"),e("a","latest"))));}
}
