package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class VodTrackPreferenceTest {
    @Test public void identicalAudioMetadataRetainsChosenProviderVariant() {
        String chosen = VodTrackPreference.identified(id(2,128000,0), "audio-3", "rep-3");
        for (int i = 0; i < 4; i++) {
            String other = VodTrackPreference.identified(id(2,128000,0), "audio-" + i, "rep-" + i);
            if (i != 3) assertTrue(VodTrackPreference.matchScore(chosen, chosen)
                    > VodTrackPreference.matchScore(chosen, other));
        }
        assertEquals("es-es", VodTrackPreference.language(chosen));
    }

    @Test public void changedGroupIdStillPrefersSameRepresentation() {
        String saved = VodTrackPreference.identified(id(2,128000,0), "old", "B");
        String chosen = VodTrackPreference.identified(id(2,128000,0), "new", "B");
        String wrong = VodTrackPreference.identified(id(2,128000,0), "new", "A");
        assertTrue(VodTrackPreference.matchScore(saved, chosen) > VodTrackPreference.matchScore(saved, wrong));
    }

    @Test public void legacyPreferencesAndChangedIdsRemainCompatible() {
        String old = id(2,128000,0);
        String modern = VodTrackPreference.identified(old, "group", "id");
        assertEquals(1000, VodTrackPreference.matchScore(old, modern));
        assertEquals(1000, VodTrackPreference.matchScore(modern,
                VodTrackPreference.identified(old, "different", "different")));
        assertEquals(0, VodTrackPreference.matchScore("v2:999:bad", modern));
        assertEquals(0, VodTrackPreference.matchScore(modern, VodTrackPreference.identified(
                VodTrackPreference.identity("en-US", "", "audio/aac", "",2,128000,0,0), "group", "id")));
    }

    @Test public void missingIdsNeverInventExactMatches() {
        String saved = VodTrackPreference.identified(id(2,128000,0), null, null);
        assertEquals(1000, VodTrackPreference.matchScore(saved, saved));
        assertEquals(0, VodTrackPreference.matchScore("invalid", "invalid"));
    }
    @Test public void distinguishesTechnicalVariantsAndForcedText() {
        assertNotEquals(id(2,128000,0),id(6,128000,0));
        assertNotEquals(id(2,128000,0),id(2,64000,0));
        assertNotEquals(id(2,128000,0),id(2,128000,2));
        assertEquals(id(2,128000,0),id(2,128000,0));
    }
    private String id(int channels,int bitrate,int flags) {
        return VodTrackPreference.identity("es-ES","Español","audio/aac","mp4a.40.2",channels,bitrate,0,flags);
    }

    @Test public void exactVariantWinsAfterManifestReorder() {
        String chosen = id(6, 256000, 0);
        assertEquals(1000, VodTrackPreference.matchScore(chosen, chosen));
        assertTrue(VodTrackPreference.matchScore(chosen, id(2,128000,0)) > 0);
        assertTrue(VodTrackPreference.matchScore(chosen, id(2,128000,0)) < 1000);
    }

    @Test public void encodingChangesDoNotLoseLanguage() {
        String saved = VodTrackPreference.identity("en-US","English","audio/eac3","ec-3",6,256000,0,0);
        String changed = VodTrackPreference.identity("en-US","English","audio/aac","mp4a.40.2",2,128000,0,1);
        assertTrue(VodTrackPreference.matchScore(saved, changed) > 0);
        assertEquals(0, VodTrackPreference.matchScore(saved, id(6,256000,0)));
    }

    @Test public void spanishRegionsAndAccessibilityRolesStayDistinct() {
        String saved = id(2,128000,0);
        assertEquals(0, VodTrackPreference.matchScore(saved, VodTrackPreference.identity(
                "es-419","Español","audio/aac","mp4a.40.2",2,128000,0,0)));
        assertEquals(0, VodTrackPreference.matchScore(saved, VodTrackPreference.identity(
                "es-ES","Español","audio/aac","mp4a.40.2",2,128000,512,0)));
    }

    @Test public void forcedTextNeverReplacesManualFullText() {
        String full = VodTrackPreference.identity("es-ES","Castellano","text/vtt","",-1,-1,0,0);
        String forced = VodTrackPreference.identity("es-ES","Castellano","text/vtt","",-1,-1,0,2);
        assertEquals(0, VodTrackPreference.matchScore(full, forced));
        assertEquals(1000, VodTrackPreference.matchScore(full, full));
    }

    @Test public void malformedAndEmptyPreferencesAreSafe() {
        for (String value : new String[]{null,"","invalid","-1:a","999999999999:x"}) {
            assertEquals(0, VodTrackPreference.matchScore(value,id(2,128000,0)));
            assertEquals("", VodTrackPreference.language(value));
        }
        assertEquals("es-es", VodTrackPreference.language(id(2,128000,0)));
    }
}
