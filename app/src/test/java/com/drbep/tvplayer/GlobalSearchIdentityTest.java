package com.drbep.tvplayer;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class GlobalSearchIdentityTest {
    static MainActivity.GlobalSearchResult row(String title, String source) {
        MainActivity.GlobalSearchResult row=new MainActivity.GlobalSearchResult(1,title,"","",null,null,null,"");
        row.searchSource=source;
        return row;
    }
    static final class Host implements GlobalSearchUiFactory.Host {
        MainActivity.GlobalSearchResult opened;
        public String text(int id){return "";}
        public String displayName(ChannelItem item){return "";}
        public String recordingTitle(RecordingsRepository.RecordingItem item){return "";}
        public String protectedTitle(ChannelItem item,String title){return title;}
        public String protectedMeta(ChannelItem item,String meta){return meta;}
        public String protectedBadge(ChannelItem item,String badge){return badge;}
        public String filterLabel(int id){return "";}
        public int[] filters(){return new int[0];}
        public int currentFilter(){return 0;}
        public boolean recordingsDisabled(){return false;}
        public boolean isHeader(MainActivity.GlobalSearchResult result){return result.type==99;}
        public boolean isHistory(MainActivity.GlobalSearchResult result){return false;}
        public void applyFilter(int filter){}
        public void applyQuery(String query){}
        public void rememberQuery(String query){}
        public void openResult(MainActivity.GlobalSearchResult result){opened=result;}
        public void openActions(MainActivity.GlobalSearchResult result){}
    }
    @Test public void insertionBeforeFocusedRowKeepsItsKey() {
        Host host=new Host();
        MainActivity.GlobalSearchResult guide=row("Programa","epg"),movie=row("Película","movies");
        String before=GlobalSearchUiFactory.build("a",Arrays.asList(guide),host).items.get(0).key;
        String after=GlobalSearchUiFactory.build("a",Arrays.asList(movie,guide),host).items.get(1).key;
        assertEquals(before,after);
    }
    @Test public void identicalTitlesFromDifferentOriginsKeepDistinctActionsAndKeys() {
        Host host=new Host();
        MainActivity.GlobalSearchResult movie=row("Título","movies"),series=row("Título","series");
        GlobalSearchListUiModel model=GlobalSearchUiFactory.build("a",Arrays.asList(movie,series),host);
        assertNotEquals(model.items.get(0).key,model.items.get(1).key);
        model.items.get(1).onClick.run();
        assertSame(series,host.opened);
    }
    @Test public void repeatedHeaderKeysAreUnique() {
        Host host=new Host();
        MainActivity.GlobalSearchResult header=new MainActivity.GlobalSearchResult(99,"Guía","","",null,null,null,"");
        GlobalSearchListUiModel model=GlobalSearchUiFactory.build("a",Arrays.asList(header,header),host);
        assertNotEquals(model.items.get(0).key,model.items.get(1).key);
    }
}
