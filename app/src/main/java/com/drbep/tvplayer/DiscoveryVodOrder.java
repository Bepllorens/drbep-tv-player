package com.drbep.tvplayer;

import java.util.List;

/** A single chronology across authorized sources, without provider quotas. */
final class DiscoveryVodOrder {
    static final int LIMIT = 40;

    static int dateKey(String date, int year) {
        int key = VodCatalogOrder.dateKey(date);
        return key != 0 ? key : VodCatalogOrder.dateKey(Integer.toString(year));
    }

    static void newest(List<StartupHomeHubUiModel.ContinueCard> rows) {
        rows.sort((a, b) -> {
            int date = Integer.compare(b.releaseDateKey, a.releaseDateKey);
            if (date != 0) return date;
            int title = a.title.compareToIgnoreCase(b.title);
            if (title != 0) return title;
            return a.subtitle.compareToIgnoreCase(b.subtitle);
        });
        // Keep a bounded discovery page; full catalogs remain in the browser.
        if (rows.size() > LIMIT) rows.subList(LIMIT, rows.size()).clear();
    }
}
