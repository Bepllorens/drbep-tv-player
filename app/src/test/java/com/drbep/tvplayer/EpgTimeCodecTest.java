package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.Test;

public class EpgTimeCodecTest {
    @Test
    public void explicitOffsetIsIndependentFromDeviceZone() {
        assertEquals(
                Instant.parse("2026-03-29T00:30:00Z").toEpochMilli(),
                EpgTimeCodec.parseEpochMillis("2026-03-29T03:30:00+03:00", ZoneId.of("Europe/Madrid"))
        );
    }

    @Test
    public void localTimeInsideDstGapMovesToFirstValidWallClockTime() {
        assertEquals(
                Instant.parse("2026-03-29T01:30:00Z").toEpochMilli(),
                EpgTimeCodec.parseEpochMillis("2026-03-29T02:30:00", ZoneId.of("Europe/Madrid"))
        );
    }

    @Test
    public void repeatedDstHourUsesEarlierOffsetDeterministically() {
        assertEquals(
                Instant.parse("2026-10-25T00:30:00Z").toEpochMilli(),
                EpgTimeCodec.parseEpochMillis("2026-10-25T02:30:00", ZoneId.of("Europe/Madrid"))
        );
    }

    @Test
    public void supportsXmlTvOffsetAndLegacyLocalFormats() {
        assertEquals(
                Instant.parse("2026-08-29T10:00:00Z").toEpochMilli(),
                EpgTimeCodec.parseEpochMillis("20260829120000 +0200", ZoneId.of("UTC"))
        );
        assertEquals(
                Instant.parse("2026-08-29T10:00:00Z").toEpochMilli(),
                EpgTimeCodec.parseEpochMillis("2026-08-29 12:00:00", ZoneId.of("Europe/Madrid"))
        );
    }

    @Test
    public void normalizesOffsetsAndEpochSecondsToUtc() {
        assertEquals("2026-08-29T10:00:00Z", EpgTimeCodec.normalizeUtc("2026-08-29T12:00:00+02:00"));
        assertEquals(1_772_363_696_000L, EpgTimeCodec.parseEpochMillis("1772363696", ZoneId.of("UTC")));
    }

    @Test
    public void invalidOrEmptyInputDoesNotInventATime() {
        assertEquals(0L, EpgTimeCodec.parseEpochMillis("not-a-date", ZoneId.of("UTC")));
        assertEquals("not-a-date", EpgTimeCodec.normalizeUtc(" not-a-date "));
        assertEquals("", EpgTimeCodec.normalizeUtc(""));
    }
}
