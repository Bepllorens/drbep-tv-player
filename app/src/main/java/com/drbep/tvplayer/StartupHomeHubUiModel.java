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
    public MainRailLayout railLayout = MainRailLayout.defaults();
    public Discovery discovery;
    public ContinueCard currentLive;
    public boolean livePlaybackError;
    public final List<ContinueCard> homeChannels = new ArrayList<>();
    public java.util.function.Supplier<List<ContinueCard>> deferredHomeChannels;

    /** UI thread only. TV refreshes need not build the hidden landing feed. */
    public List<ContinueCard> resolveHomeChannels() {
        if (deferredHomeChannels != null) {
            List<ContinueCard> resolved = deferredHomeChannels.get();
            deferredHomeChannels = null;
            homeChannels.clear();
            if (resolved != null) homeChannels.addAll(resolved);
        }
        return homeChannels;
    }

    public Runnable onSavedArchive;
    public Runnable onLiveGuide, onLiveRecord, onLiveStartOver, onLiveHistory;
    public String liveGroupTitle = "";

    public static final class Discovery {
        public final List<Provider> providers = new ArrayList<>();
        public final List<Provider> liveProviders = new ArrayList<>();
        public final java.util.Map<String, Integer> scrollPositions = new java.util.HashMap<>();
        public java.util.function.BiFunction<String, String, Page> page;
        public String section = "Inicio";
        public String selectedLiveChannel = "";
        public final java.util.Map<String, String> selectedLiveChannels = new java.util.HashMap<>();
        public String liveQuery = "";
        public String homeFocusedChannel = "";
        public boolean startupPreviewFocusPending = true;
        public Runnable cancelLiveOrder;
        public String librarySelectedId = "";
        public Runnable dismissLibraryActions;
        public Runnable refreshHome;
        public Runnable requestExit;
        public java.util.function.Consumer<String> selectLiveProvider;
        public java.util.function.BiConsumer<String, java.util.List<String>> saveChannelOrder;
        public java.util.function.Consumer<java.util.List<String>> saveLiveOrder;
        public boolean restoreLiveFocus = false;
        public final java.util.Map<String, String> selectedProviders = new java.util.HashMap<>();
    }
    public static final class Provider {
        public final String key, title, logo;
        public Provider(String key, String title, String logo) {
            this.key=key; this.title=title; this.logo=safe(logo);
        }
    }
    public static final class Page {
        public String heading = "", description = "", actionLabel = "Explorar catálogo";
        public final List<ContinueCard> cards = new ArrayList<>();
        public Runnable browse;
    }

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
        public final String navigationId;
        public final String eyebrow;
        public final String title;
        public final String subtitle;
        public final String metric;
        public final boolean vod;
        public final Runnable onClick;

        public PrimaryCard(String eyebrow, String title, String subtitle, String metric, boolean vod, Runnable onClick) {
            this(eyebrow, title, subtitle, metric, vod, onClick, "");
        }
        public PrimaryCard(String eyebrow, String title, String subtitle, String metric, boolean vod, Runnable onClick, String navigationId) {
            this.navigationId = safe(navigationId);
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
        public ProgrammeMetadataUiModel programmeMetadata;
        public java.util.function.Supplier<ContinueCard> refreshProgramme;
        public long programmeSnapshotUptimeMs;
        public String channelId = "", description = "", schedule = "", nextProgram = "";
        public String libraryId = "", facts = "";
        public Runnable onResume, onStartOver, onRemove, onEpisodes;
        public String kind = "";
        public String providerKey = "";
        public int releaseDateKey;
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

    public static List<ContinueCard> continueGroup(List<ContinueCard> cards, boolean live) {
        List<ContinueCard> result=new ArrayList<>();
        if(cards!=null)for(ContinueCard card:cards)if(card!=null&&card.livePreview==live)result.add(card);
        return result;
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
