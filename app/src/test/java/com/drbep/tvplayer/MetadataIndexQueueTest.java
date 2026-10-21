package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;

public class MetadataIndexQueueTest {
    @Test public void queueProjectionKeepsBoundParametersWithoutPresentationJoins() {
        for(boolean series:new boolean[]{false,true})for(boolean group:new boolean[]{false,true}) {
            String sql=MetadataIndexQueue.sql(series,group);
            assertEquals(group?5:4,sql.chars().filter(c->c=='?').count());
            assertTrue(sql.startsWith("INSERT OR IGNORE INTO metadata_index"));
            assertFalse(sql.contains("movie_metadata"));assertFalse(sql.contains("movie_match"));
            assertFalse(sql.contains("series_match"));assertFalse(sql.contains("resume_ms"));
            assertTrue(sql.contains("COALESCE(s.active,s.preview)=e.generation"));
            assertTrue(sql.contains("COALESCE(p.hidden,0)=0"));
        }
    }
    @Test public void seriesKeepsVisibleRepresentativeAndGroupMembershipSeparate() {
        String sql=MetadataIndexQueue.sql(true,true);
        assertTrue(sql.contains("MIN(e.rowid) AS representative_id"));
        assertTrue(sql.contains("GROUP BY e.source,e.generation,e.series"));
        assertTrue(sql.contains(MetadataIndexSelection.groupPredicate("e","series")));
        assertFalse(MetadataIndexQueue.sql(false,false).contains("GROUP BY"));
    }
    @Test public void selectedSeriesFiltersBeforeGroupingAndBindsSourceAsData() {
        String source="list' OR 1=1 --";
        String sql=MetadataIndexQueue.sql(true,true,true);
        assertTrue(sql.indexOf("AND e.source=? GROUP BY") < sql.indexOf("JOIN entries e ON"));
        assertFalse(sql.contains(source));
        assertArrayEquals(new String[]{"series",source,"series",source,source,"Drama"},
                MetadataIndexQueue.args(true,"series",source,"Drama"));
        assertEquals(6,sql.chars().filter(c->c=='?').count());
    }
    @Test public void allSourcesAndMoviesKeepPreviousQueriesAndBindings() {
        for(boolean group:new boolean[]{false,true}) {
            assertEquals(MetadataIndexQueue.sql(true,group),MetadataIndexQueue.sql(true,group,false));
            assertEquals(MetadataIndexQueue.sql(false,group),MetadataIndexQueue.sql(false,group,true));
        }
        assertArrayEquals(new String[]{"series","series","",""},MetadataIndexQueue.args(true,"series","",""));
        assertArrayEquals(new String[]{"movie","movie","a","a"},MetadataIndexQueue.args(false,"movie","a",""));
    }
}
