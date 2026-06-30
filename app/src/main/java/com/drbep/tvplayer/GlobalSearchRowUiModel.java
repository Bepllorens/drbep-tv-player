package com.drbep.tvplayer;

public final class GlobalSearchRowUiModel {
    public static final int IMAGE_NONE = 0;
    public static final int IMAGE_CHANNEL = 1;
    public static final int IMAGE_PROGRAM = 2;
    public static final int IMAGE_RECORDING = 3;

    public final String title;
    public final String meta;
    public final String badge;
    public final boolean header;
    public final int imageKind;
    public final String imageUrl;
    public final String imageName;
    public final Runnable onClick;
    public final Runnable onLongClick;

    public GlobalSearchRowUiModel(
            String title,
            String meta,
            String badge,
            boolean header,
            int imageKind,
            String imageUrl,
            String imageName,
            Runnable onClick,
            Runnable onLongClick
    ) {
        this.title = title == null ? "" : title;
        this.meta = meta == null ? "" : meta;
        this.badge = badge == null ? "" : badge;
        this.header = header;
        this.imageKind = imageKind;
        this.imageUrl = imageUrl == null ? "" : imageUrl;
        this.imageName = imageName == null ? "" : imageName;
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }
}
