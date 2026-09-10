package com.drbep.tvplayer;
import org.junit.Test;
import org.json.JSONObject;
import org.json.JSONArray;
import static org.junit.Assert.*;
public class HboWatchHistoryTest {
    JSONObject episode(String id,int season,int number)throws Exception{return new JSONObject().put("id",id).put("title","Episode "+number).put("kind","episode").put("series_id","series1").put("series_title","Series").put("season",season).put("episode",number).put("duration_seconds",1200);}
    JSONObject payload()throws Exception{return new JSONObject().put("vod_progress",new JSONObject()).put("series_continuity",new JSONObject()).put("watched",new JSONArray());}
    @Test public void savesResumeAndGroupsOneCardPerSeries()throws Exception{
        HboWatchHistory h=new HboWatchHistory();h.remember(episode("a",1,1));h.record("hbomax:a",123000,1200000,100);
        h.remember(episode("b",1,2));h.record("hbomax:b",61000,1200000,200);
        HboWatchHistory restored=new HboWatchHistory();restored.load(h.save());
        assertEquals(123000,restored.get("a").position);assertEquals(1,restored.continuing().size());assertEquals("b",restored.continuing().get(0).id);
    }
    @Test public void completionOffersNextSeasonWithoutReturningToFinishedEpisode()throws Exception{
        HboWatchHistory h=new HboWatchHistory();h.remember(episode("a",1,10));assertTrue(h.record("a",1180000,1200000,100));
        h.next("a",episode("b",2,1));h.record("a",1190000,1200000,200);
        assertEquals("Visto",h.badge("a"));assertEquals("b",h.continuing().get(0).id);assertTrue(h.continuing().get(0).pending);
        h.record("b",45000,1200000,300);assertFalse(h.continuing().get(0).pending);
    }
    @Test public void staleNextRequestDoesNotReplaceUserChoice()throws Exception{
        HboWatchHistory h=new HboWatchHistory();h.remember(episode("a",1,1));h.record("a",1180000,1200000,100);
        h.remember(episode("c",2,4));h.record("c",60000,1200000,200);h.next("a",episode("b",1,2));
        assertEquals("c",h.continuing().get(0).id);
    }
    @Test public void serverRoundTripRestoresEpisodeAndPendingNext()throws Exception{
        HboWatchHistory h=new HboWatchHistory();h.remember(episode("a",1,1));h.record("a",123000,1200000,100);
        JSONObject payload=payload();h.exportInto(payload);HboWatchHistory second=new HboWatchHistory();second.merge(payload);
        assertEquals(123000,second.continuing().get(0).position);assertEquals("Series",second.continuing().get(0).seriesTitle);
        h.record("a",1180000,1200000,200);h.next("a",episode("b",1,2));payload=payload();h.exportInto(payload);second.merge(payload);
        assertEquals("b",second.continuing().get(0).id);assertEquals("Visto",second.badge("a"));
    }
    @Test public void olderRemotePositionDoesNotOverwriteLocal()throws Exception{
        HboWatchHistory h=new HboWatchHistory();h.remember(episode("a",1,1));h.record("a",61000,1200000,100);JSONObject p=payload();h.exportInto(p);
        h.record("a",123000,1200000,200);h.merge(p);assertEquals(123000,h.get("a").position);
    }
    @Test public void finishedFinalEpisodeHasNoContinueCard()throws Exception{
        HboWatchHistory h=new HboWatchHistory();h.remember(episode("a",1,1));h.record("a",1180000,1200000,100);assertTrue(h.continuing().isEmpty());
    }
}
