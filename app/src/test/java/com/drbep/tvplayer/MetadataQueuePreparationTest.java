package com.drbep.tvplayer;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MetadataQueuePreparationTest {
    static class Tx implements MetadataIndexQueue.Transaction {
        final List<String> events=new ArrayList<>();
        boolean failEnd;
        public void begin(){events.add("begin");}
        public void successful(){events.add("commit");}
        public void end(){events.add("end");if(failEnd)throw new IllegalStateException();}
    }
    @Test public void markerFollowsDatabaseCommit() {
        Tx t=new Tx();
        assertTrue(MetadataIndexQueue.prepare(t,()->true,()->t.events.add("rebuild"),()->t.events.add("marker")));
        assertEquals(Arrays.asList("begin","rebuild","commit","end","marker"),t.events);
    }
    @Test public void failedRebuildNeverCommitsOrMarks() {
        Tx t=new Tx();
        try{MetadataIndexQueue.prepare(t,()->true,()->{throw new IllegalStateException();},()->fail());fail();}
        catch(IllegalStateException expected){}
        assertEquals(Arrays.asList("begin","end"),t.events);
    }
    @Test public void stoppedBeforeStartDoesNotWrite() {
        Tx t=new Tx();
        assertFalse(MetadataIndexQueue.prepare(t,()->false,()->fail(),()->fail()));
        assertTrue(t.events.isEmpty());
    }
    @Test public void stoppedDuringRebuildRollsBackWithoutMarker() {
        Tx t=new Tx();boolean[] active={true};
        assertFalse(MetadataIndexQueue.prepare(t,()->active[0],()->active[0]=false,()->fail()));
        assertEquals(Arrays.asList("begin","end"),t.events);
    }
    @Test public void commitFailureDoesNotClearPreparationFlag() {
        Tx t=new Tx();t.failEnd=true;
        try{MetadataIndexQueue.prepare(t,()->true,()->{},()->fail());fail();}
        catch(IllegalStateException expected){}
        assertEquals(Arrays.asList("begin","commit","end"),t.events);
    }
    @Test public void markerFailurePropagatesForRetryAfterSqlCommit() {
        Tx t=new Tx();
        try{MetadataIndexQueue.prepare(t,()->true,()->{},()->{throw new IllegalStateException();});fail();}
        catch(IllegalStateException expected){}
        assertEquals(Arrays.asList("begin","commit","end"),t.events);
    }
}
