package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

public class StartupHomeHubUiModelTest {
    @Test
    public void includesVodWhenRemoteCountArrivesBeforeFullCatalog() {
        assertTrue(StartupHomeHubUiModel.shouldIncludeVodCard(2901, false));
    }

    @Test
    public void includesVodWhenCatalogAlreadyContainsVod() {
        assertTrue(StartupHomeHubUiModel.shouldIncludeVodCard(0, true));
    }

    @Test
    public void hidesVodOnlyWhenNeitherSourceReportsTitles() {
        assertFalse(StartupHomeHubUiModel.shouldIncludeVodCard(0, false));
    }

    @Test
    public void keepsRecommendationsInTheirOwnHomeSection() {
        StartupHomeHubUiModel.ContinueCard recommendation = new StartupHomeHubUiModel.ContinueCard(
                "Próximo programa", "Favorito · Canal · 21:00", "", "Canal", false, 0f, null);
        StartupHomeHubUiModel model = new StartupHomeHubUiModel(
                "DRBEP", "21:00", "10 canales", "Inicio", "",
                Collections.emptyList(), Collections.emptyList(), Collections.singletonList(recommendation),
                Collections.emptyList(), null, null, null);

        assertTrue(model.continueCards.isEmpty());
        assertEquals(1, model.recommendationCards.size());
        assertEquals("Próximo programa", model.recommendationCards.get(0).title);
    }
    private StartupHomeHubUiModel emptyModel() {
        return new StartupHomeHubUiModel("DRBEP", "", "", "", "",
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null, null, null);
    }

    @Test public void hiddenHomeFeedWaitsUntilRequestedAndKeepsItsOrder() {
        StartupHomeHubUiModel model = emptyModel();
        int[] reads = {0};
        StartupHomeHubUiModel.ContinueCard first = new StartupHomeHubUiModel.ContinueCard("A", "", "", "", false, 0, null);
        StartupHomeHubUiModel.ContinueCard second = new StartupHomeHubUiModel.ContinueCard("B", "", "", "", false, 0, null);
        model.deferredHomeChannels = () -> { reads[0]++; return java.util.Arrays.asList(first, second); };
        assertEquals(0, reads[0]);
        assertEquals(java.util.Arrays.asList(first, second), model.resolveHomeChannels());
        assertEquals(java.util.Arrays.asList(first, second), model.resolveHomeChannels());
        assertEquals(1, reads[0]);
    }

    @Test public void failedDeferredFeedCanRetryWithoutLosingExistingRows() {
        StartupHomeHubUiModel model = emptyModel();
        StartupHomeHubUiModel.ContinueCard existing = new StartupHomeHubUiModel.ContinueCard("A", "", "", "", false, 0, null);
        model.homeChannels.add(existing);
        int[] attempts = {0};
        model.deferredHomeChannels = () -> {
            if (attempts[0]++ == 0) throw new IllegalStateException("transient");
            return Collections.singletonList(existing);
        };
        try { model.resolveHomeChannels(); org.junit.Assert.fail("Expected failure"); }
        catch (IllegalStateException expected) { assertEquals("transient", expected.getMessage()); }
        assertEquals(Collections.singletonList(existing), model.homeChannels);
        assertEquals(Collections.singletonList(existing), model.resolveHomeChannels());
        assertEquals(2, attempts[0]);
    }

    @Test public void eagerHomeFeedIsNotChanged() {
        StartupHomeHubUiModel model = emptyModel();
        StartupHomeHubUiModel.ContinueCard existing = new StartupHomeHubUiModel.ContinueCard("A", "", "", "", false, 0, null);
        model.homeChannels.add(existing);
        assertEquals(Collections.singletonList(existing), model.resolveHomeChannels());
    }
}
