package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
}
