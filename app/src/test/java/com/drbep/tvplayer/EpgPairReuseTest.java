package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.time.Instant;
import com.drbep.tvplayer.EpgRepository.EpgProgram;
import com.drbep.tvplayer.EpgRepository.EpgProgramPair;

public class EpgPairReuseTest {
    static final long BASE=1792740000000L;
    static EpgProgram program(String title, long start, long end, int progress) {
        return new EpgProgram("ch", "Channel", "official", title, "image", "description",
            start==0?"":Instant.ofEpochMilli(start).toString(), end==0?"":Instant.ofEpochMilli(end).toString(),
            "Cine", progress, new ProgrammeMetadata("Cine","2026","12","4","description"), "details");
    }
    static void same(EpgProgram expected,EpgProgram actual) {
        if(expected==null){assertNull(actual);return;}
        assertNotNull(actual);
        assertEquals(expected.title,actual.title);assertEquals(expected.progress,actual.progress);
        assertEquals(expected.startTime,actual.startTime);assertEquals(expected.endTime,actual.endTime);
        assertEquals(expected.icon,actual.icon);assertEquals(expected.description,actual.description);
        assertEquals(expected.channelId,actual.channelId);assertEquals(expected.channelName,actual.channelName);
        assertEquals(expected.tvgId,actual.tvgId);assertEquals(expected.category,actual.category);
        assertSame(expected.metadata,actual.metadata);assertEquals(expected.detailsUrl,actual.detailsUrl);
    }
    @Test public void selectionMatchesPreviousAlgorithmAcrossBoundariesAndOverlaps() {
        List<EpgProgram> candidates=Arrays.asList(null,program("untimed",0,0,-1),
            program("missing-end",BASE,0,35),program("expired",BASE-2000,BASE-1000,50),
            program("current",BASE-1000,BASE+1000,50),program("future",BASE+1000,BASE+3000,0),
            program("overlap",BASE-500,BASE+2000,2),program("inverted",BASE+2000,BASE+1000,0));
        long[] times={BASE-3000,BASE-1000,BASE-1,BASE,BASE+1,BASE+1000,BASE+2000,BASE+4000};
        assertNull(EpgRepository.normalizePairForNow(null,BASE));
        for(EpgProgram current:candidates)for(EpgProgram next:candidates)for(long now:times){
            EpgProgramPair pair=new EpgProgramPair(current,next),expected=referenceNormalize(pair,now),actual=EpgRepository.normalizePairForNow(pair,now);
            same(expected.current,actual.current);same(expected.next,actual.next);
        }
    }
    @Test public void unchangedImmutableProgramsAndPairAreReusedButProgressStillAdvances() {
        EpgProgram current=program("current",BASE,BASE+100000,50),next=program("next",BASE+100000,BASE+200000,-1);
        EpgProgramPair pair=new EpgProgramPair(current,next);
        assertSame(pair,EpgRepository.normalizePairForNow(pair,BASE+50000));
        EpgProgramPair changed=EpgRepository.normalizePairForNow(pair,BASE+51000);
        assertNotSame(pair,changed);assertEquals(51,changed.current.progress);assertSame(next,changed.next);
        assertEquals(50,current.progress);
        EpgProgramPair boundary=EpgRepository.normalizePairForNow(pair,BASE+100000);
        assertEquals("next",boundary.current.title);assertNull(boundary.next);assertEquals(0,boundary.current.progress);
    }
    @Test public void cooldownEarlyDecisionMatchesFullDecisionForEveryCoverage() {
        for(long last:new long[]{0,90000,100000,110000})for(long now:new long[]{99999,100000,135000,145000})
            for(long cooldown:new long[]{-1,0,45000})for(int total=0;total<10;total++)for(int resolved=0;resolved<=total;resolved++) {
                boolean early=MainActivity.shouldHydrateVisibleEpg(1,0,last,now,cooldown);
                boolean expected=MainActivity.shouldHydrateVisibleEpg(total,resolved,last,now,cooldown);
                assertEquals(expected,early && MainActivity.shouldHydrateVisibleEpg(total,resolved,last,now,cooldown));
            }
    }
    static EpgProgramPair referenceSelect(List<EpgProgram> programs, long now) {
        EpgProgram current = null;
        EpgProgram next = null;
        if (programs == null) {
            return new EpgProgramPair(null, null);
        }
        for (EpgProgram program : programs) {
            if (program == null) {
                continue;
            }
            long startMs = EpgTimeCodec.parseEpochMillis(program.startTime);
            long endMs = EpgTimeCodec.parseEpochMillis(program.endTime);
            if (current == null && startMs <= now && endMs > now) {
                current = referenceProgress(program, now);
                continue;
            }
            if (startMs > now && (next == null || startMs < EpgTimeCodec.parseEpochMillis(next.startTime))) {
                next = referenceProgress(program, now);
            }
        }
        return new EpgProgramPair(current, next);
    }

    static EpgProgramPair referenceNormalize(EpgProgramPair pair, long now) {
        if (pair == null) {
            return null;
        }
        List<EpgProgram> timed = new ArrayList<>();
        if (hasProgramTiming(pair.current)) {
            timed.add(pair.current);
        }
        if (hasProgramTiming(pair.next)) {
            timed.add(pair.next);
        }
        EpgProgramPair selected = referenceSelect(timed, now);
        EpgProgram current = selected.current;
        EpgProgram next = selected.next;
        if (current == null && pair.current != null && !hasProgramTiming(pair.current)) {
            current = pair.current;
        }
        if (next == null && pair.next != null && !hasProgramTiming(pair.next)) {
            next = pair.next;
        }
        return new EpgProgramPair(current, next);
    }

    private static boolean hasProgramTiming(EpgProgram program) {
        return program != null
                && EpgTimeCodec.parseEpochMillis(program.startTime) > 0L
                && EpgTimeCodec.parseEpochMillis(program.endTime) > 0L;
    }

    private static EpgProgram referenceProgress(EpgProgram program, long nowMs) {
        if (program == null) {
            return null;
        }
        long startMs = EpgTimeCodec.parseEpochMillis(program.startTime);
        long endMs = EpgTimeCodec.parseEpochMillis(program.endTime);
        int progress = -1;
        if (startMs > 0 && endMs > startMs && startMs <= nowMs && endMs > nowMs) {
            progress = (int) Math.max(0L, Math.min(100L, ((nowMs - startMs) * 100L) / (endMs - startMs)));
        }
        return new EpgProgram(program.channelId, program.channelName, program.tvgId, program.title, program.icon, program.description, program.startTime, program.endTime, program.category, progress, program.metadata, program.detailsUrl);
    }


}
