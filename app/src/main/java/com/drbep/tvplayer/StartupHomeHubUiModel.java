package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class StartupHomeHubUiModel {
    static boolean shouldIncludeVodCard(int knownVodCount, boolean catalogHasVod) {
        return knownVodCount > 0 || catalogHasVod;
    }

    public final String brand;
    public final String clock;
    public final String catalogSummary;
    public final String title;
    public final String subtitle;
    public final List<PrimaryCard> primaryCards;
    public final List<ContinueCard> continueCards;
    public final List<ContinueCard> recommendationCards;
    public final List<Shortcut> shortcuts;
    public final Runnable onSearch;
    public final Runnable onSettings;
    public final Runnable onBack;

    public StartupHomeHubUiModel(
            String brand,
            String clock,
            String catalogSummary,
            String title,
            String subtitle,
            List<PrimaryCard> primaryCards,
            List<ContinueCard> continueCards,
            List<ContinueCard> recommendationCards,
            List<Shortcut> shortcuts,
            Runnable onSearch,
            Runnable onSettings,
            Runnable onBack
    ) {
        this.brand = safe(brand);
        this.clock = safe(clock);
        this.catalogSummary = safe(catalogSummary);
        this.title = safe(title);
        this.subtitle = safe(subtitle);
        this.primaryCards = primaryCards == null ? new ArrayList<>() : new ArrayList<>(primaryCards);
        this.continueCards = continueCards == null ? new ArrayList<>() : new ArrayList<>(continueCards);
        this.recommendationCards = recommendationCards == null ? new ArrayList<>() : new ArrayList<>(recommendationCards);
        this.shortcuts = shortcuts == null ? new ArrayList<>() : new ArrayList<>(shortcuts);
        this.onSearch = onSearch;
        this.onSettings = onSettings;
        this.onBack = onBack;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class PrimaryCard {
        public final String eyebrow;
        public final String title;
        public final String subtitle;
        public final String metric;
        public final boolean vod;
        public final Runnable onClick;

        public PrimaryCard(String eyebrow, String title, String subtitle, String metric, boolean vod, Runnable onClick) {
            this.eyebrow = safe(eyebrow);
            this.title = safe(title);
            this.subtitle = safe(subtitle);
            this.metric = safe(metric);
            this.vod = vod;
            this.onClick = onClick;
        }
    }

    public static final class ContinueCard {
        public final String title;
        public final String subtitle;
        public final String imageUrl;
        public final String imageLabel;
        public final boolean poster;
        public final boolean livePreview;
        public final float progress;
        public final Runnable onClick;

        public ContinueCard(String title, String subtitle, String imageUrl, String imageLabel, boolean poster, float progress, Runnable onClick) {
            this(title, subtitle, imageUrl, imageLabel, poster, false, progress, onClick);
        }

        public ContinueCard(String title, String subtitle, String imageUrl, String imageLabel, boolean poster, boolean livePreview, float progress, Runnable onClick) {
            this.title = safe(title);
            this.subtitle = safe(subtitle);
            this.imageUrl = safe(imageUrl);
            this.imageLabel = safe(imageLabel);
            this.poster = poster;
            this.livePreview = livePreview;
            this.progress = Math.max(0f, Math.min(1f, progress));
            this.onClick = onClick;
        }
    }

    public static final class Shortcut {
        public final String icon;
        public final String title;
        public final String subtitle;
        public final Runnable onClick;

        public Shortcut(String icon, String title, String subtitle, Runnable onClick) {
            this.icon = safe(icon);
            this.title = safe(title);
            this.subtitle = safe(subtitle);
            this.onClick = onClick;
        }
    }
}
