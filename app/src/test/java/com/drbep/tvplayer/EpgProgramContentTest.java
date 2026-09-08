package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class EpgProgramContentTest {
    private EpgRepository.EpgProgram programme(String title, String description, String end, int progress) {
        return new EpgRepository.EpgProgram("1", "Channel", title, "icon", description,
                "2026-09-05T09:00:00Z", end, progress);
    }

    @Test public void equivalentResponseAndProgressDoNotInvalidateContent() {
        assertTrue(EpgRepository.sameProgramContent(null, null));
        assertTrue(EpgRepository.sameProgramContent(programme("Show", "Text", "2026-09-05T10:00:00Z", 10),
                programme("Show", "Text", "2026-09-05T10:00:00Z", 20)));
    }

    @Test public void titleDetailsAndScheduleChangesAreNotSuppressed() {
        EpgRepository.EpgProgram base = programme("Show", "Text", "2026-09-05T10:00:00Z", 10);
        assertFalse(EpgRepository.sameProgramContent(base, null));
        assertFalse(EpgRepository.sameProgramContent(base, programme("Next", "Text", "2026-09-05T10:00:00Z", 10)));
        assertFalse(EpgRepository.sameProgramContent(base, programme("Show", "Rich text", "2026-09-05T10:00:00Z", 10)));
        assertFalse(EpgRepository.sameProgramContent(base, programme("Show", "Text", "2026-09-05T11:00:00Z", 10)));
    }
}
