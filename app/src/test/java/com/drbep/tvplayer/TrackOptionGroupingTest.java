package com.drbep.tvplayer;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class TrackOptionGroupingTest {
    private TrackOptionGrouping.Candidate c(String key,boolean supported,boolean selected,int rate) {
        return new TrackOptionGrouping.Candidate(key,supported,selected,rate);
    }
    private String key(String lang,String label,String mime,String codec,int channels,int roles,int flags,int cc,String id) {
        return TrackOptionGrouping.identity(lang,label,mime,codec,channels,48000,roles,flags,cc,id);
    }
    @Test public void repeatedAdaptationRepresentationsBecomeOneOptionWithoutLosingSelection() {
        List<TrackOptionGrouping.Candidate> list=new ArrayList<>();
        for(int i=0;i<70;i++)list.add(c("es",true,i==17,64000+i*1000));
        assertEquals(Arrays.asList(17),TrackOptionGrouping.representatives(list));
    }
    @Test public void supportedAlternativeWinsAndOriginalCoordinatesRemainAvailable() {
        assertEquals(Arrays.asList(2,1),TrackOptionGrouping.representatives(Arrays.asList(
            c("es",false,false,900000),c("en",true,false,128000),c("es",true,false,64000))));
    }
    @Test public void highestBitrateIsPreferredOnlyWhenNothingInGroupIsSelected() {
        assertEquals(Arrays.asList(1),TrackOptionGrouping.representatives(Arrays.asList(c("es",true,false,64000),c("es",true,false,128000))));
    }
    @Test public void languageRegionsAndCommentaryRemainDifferent() {
        String base=key("es-ES","","audio/aac","aac",2,0,0,-1,"a");
        assertNotEquals(base,key("es-419","","audio/aac","aac",2,0,0,-1,"a"));
        assertNotEquals(base,key("es-ES","Comentarios","audio/aac","aac",2,0,0,-1,"a"));
        assertNotEquals(base,key("es-ES","","audio/aac","aac",2,512,0,-1,"a"));
    }
    @Test public void codecsSurroundAndForcedOrAccessibleSubtitlesAreNotMerged() {
        String base=key("es","","audio/aac","aac",2,0,0,-1,"a");
        assertNotEquals(base,key("es","","audio/eac3","ec-3",6,0,0,-1,"a"));
        String text=key("es","","text/vtt","",-1,0,0,-1,"a");
        assertNotEquals(text,key("es","","text/vtt","",-1,0,2,-1,"a"));
        assertNotEquals(text,key("es","","text/vtt","",-1,1024,0,-1,"a"));
        assertNotEquals(text,key("es","","text/vtt","",-1,0,0,2,"a"));
    }
    @Test public void unknownLanguagesAreNotCollapsedAndCaseIsNormalized() {
        assertNotEquals(key("","","audio/aac","",2,0,0,-1,"0:0"),key("","","audio/aac","",2,0,0,-1,"1:0"));
        assertEquals(key("ES_es"," Original ","audio/aac","",2,0,0,-1,"a"),key("es-ES","original","audio/aac","",2,0,0,-1,"b"));
        assertTrue(TrackOptionGrouping.representatives(Collections.emptyList()).isEmpty());
    }
}
