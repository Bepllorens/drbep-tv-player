package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;

public class VisibleEpgTransitionTest {
    private EpgRepository.EpgProgram p(String id,String title,long start,long end) {
        return new EpgRepository.EpgProgram(id,"Canal",title,"","",EpgTimeCodec.formatUtc(start),EpgTimeCodec.formatUtc(end),0);
    }
    @Test public void displayedProgramAdvancesAtBoundaryAndDoesNotResurrectExpiredTitle() {
        long start=1800000L;
        EpgRepository.EpgProgramPair pair=new EpgRepository.EpgProgramPair(p("a","Primero",start,start+60000),p("a","Segundo",start+60000,start+120000));
        assertEquals("Primero",OverlayChannelListUiFactory.resolveCurrentProgram(null,pair,start+59999));
        assertEquals("Segundo",OverlayChannelListUiFactory.resolveCurrentProgram(null,pair,start+60000));
        assertEquals("",OverlayChannelListUiFactory.resolveCurrentProgram(null,pair,start+120000));
    }
    @Test public void existingRowsRefreshOnlyTheirOwnProgrammeWithoutChangingSelectionOrActions() {
        long now=System.currentTimeMillis();
        java.util.Map<String,EpgRepository.EpgProgramPair> pairs=new java.util.HashMap<>();
        ChannelItem a=channel("a"), b=channel("b");
        pairs.put("a",new EpgRepository.EpgProgramPair(p("a","A actual",now-60000,now+60000),null));
        pairs.put("b",new EpgRepository.EpgProgramPair(p("b","B actual",now-60000,now+60000),null));
        int[] tuned={-1};
        OverlayChannelListUiFactory.Host host=(OverlayChannelListUiFactory.Host)java.lang.reflect.Proxy.newProxyInstance(
            getClass().getClassLoader(),new Class<?>[]{OverlayChannelListUiFactory.Host.class},(proxy,method,args)->{
                switch(method.getName()) {
                    case "channels": return java.util.Arrays.asList(a,b);
                    case "epgPair": return pairs.get(((ChannelItem)args[0]).id);
                    case "displayName": return ((ChannelItem)args[0]).id;
                    case "decorateProtectedTitle": case "decorateProtectedMeta": return args[1];
                    case "selectAndTune": tuned[0]=(Integer)args[0]; return null;
                }
                if(method.getReturnType()==String.class)return "";
                if(method.getReturnType()==boolean.class)return false;
                if(method.getReturnType()==int.class)return 0;
                return null;
            });
        OverlayChannelListUiModel model=OverlayChannelListUiFactory.build(host);
        OverlayChannelRowUiModel first=model.items.get(0), second=model.items.get(1);
        assertTrue(first.selected); assertFalse(second.selected);
        pairs.put("a",new EpgRepository.EpgProgramPair(p("a","A nuevo",now-1000,now+120000),null));
        assertEquals("A nuevo",first.refreshMeta.get());
        assertEquals("B actual",second.refreshMeta.get());
        assertEquals("A actual",first.meta); // No list replacement or mutation of the bound row.
        assertTrue(first.selected); assertFalse(second.selected);
        second.onClick.run();assertEquals(1,tuned[0]);
    }
    private ChannelItem channel(String id) {
        return new ChannelItem(id,"Prueba","","","TV","https://example.com/live.m3u8","",1,1,
            false,false,1,"Prueba",java.util.Collections.emptyList(),"","","",true);
    }
}
