package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class DefaultAudioPolicyTest {
    @Test public void spainWinsOverEnglishAndLatinAmericanSpanish() {
        assertTrue(DefaultAudioPolicy.priority("es-ES", "") > DefaultAudioPolicy.priority("es-419", ""));
        assertTrue(DefaultAudioPolicy.priority("es", "") > DefaultAudioPolicy.priority("en", ""));
        assertEquals(300, DefaultAudioPolicy.priority("ES_es", ""));
    }
    @Test public void labelsDisambiguateGenericLanguageTags() {
        assertEquals(300, DefaultAudioPolicy.priority("es", "Español (España)"));
        assertEquals(300, DefaultAudioPolicy.priority("spa", "Castellano"));
        assertEquals(100, DefaultAudioPolicy.priority("es", "Español latinoamericano"));
        assertEquals(300, DefaultAudioPolicy.priority(null, "Spanish (Spain)"));
    }
    @Test public void missingAndOtherLanguagesDoNotForceUnsupportedSpanish() {
        assertEquals(0, DefaultAudioPolicy.priority(null, null));
        assertEquals(0, DefaultAudioPolicy.priority("fr", "Français"));
        assertEquals(200, DefaultAudioPolicy.priority("spa", ""));
    }
}
