package com.drbep.tvplayer;

public final class EpgSearchResultRowUiModel {
    public final String title;
    public final String meta;
    public final String badge;
    public final int badgeColor;
    public final String imageUrl;
    public final Runnable onClick;

    public EpgSearchResultRowUiModel(
            String title,
            String meta,
            String badge,
            int badgeColor,
            String imageUrl,
            Runnable onClick
    ) {
        this.title = title == null ? "" : title;
        this.meta = meta == null ? "" : meta;
        this.badge = badge == null ? "" : badge;
        this.badgeColor = badgeColor;
        this.imageUrl = imageUrl == null ? "" : imageUrl;
        this.onClick = onClick;
    }
}
