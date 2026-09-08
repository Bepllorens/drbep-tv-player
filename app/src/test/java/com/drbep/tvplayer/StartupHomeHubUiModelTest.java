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
}
