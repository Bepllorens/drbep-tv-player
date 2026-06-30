package com.drbep.tvplayer;

public final class OverlayChannelRowUiModel {
    public final String logoUrl;
    public final String name;
    public final String meta;
    public final String badge;
    public final boolean badgeVisible;
    public final int badgeTextColor;
    public final boolean favoriteVisible;
    public final boolean favorite;
    public final String favoriteText;
    public final int favoriteTextColor;
    public final boolean selected;
    public final boolean tuned;
    public final boolean vod;
    public final String query;
    public final Runnable onClick;
    public final Runnable onFavoriteClick;

    public OverlayChannelRowUiModel(
            String logoUrl,
            String name,
            String meta,
            String badge,
            boolean badgeVisible,
            int badgeTextColor,
            boolean favoriteVisible,
            boolean favorite,
            String favoriteText,
            int favoriteTextColor,
            boolean selected,
            boolean tuned,
            boolean vod,
            String query,
            Runnable onClick,
            Runnable onFavoriteClick
    ) {
        this.logoUrl = logoUrl == null ? "" : logoUrl;
        this.name = name == null ? "" : name;
        this.meta = meta == null ? "" : meta;
        this.badge = badge == null ? "" : badge;
        this.badgeVisible = badgeVisible;
        this.badgeTextColor = badgeTextColor;
        this.favoriteVisible = favoriteVisible;
        this.favorite = favorite;
        this.favoriteText = favoriteText == null ? "" : favoriteText;
        this.favoriteTextColor = favoriteTextColor;
        this.selected = selected;
        this.tuned = tuned;
        this.vod = vod;
        this.query = query == null ? "" : query;
        this.onClick = onClick;
        this.onFavoriteClick = onFavoriteClick;
    }
}
