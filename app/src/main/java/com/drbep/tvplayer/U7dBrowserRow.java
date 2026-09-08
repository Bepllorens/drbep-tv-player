package com.drbep.tvplayer;

public final class U7dBrowserRow {
    public final String day, title, meta, description;
    public final java.util.function.Consumer<android.widget.ImageView> artwork;
    public final Runnable play;
    public U7dBrowserRow(String day, String title, String meta, String description,
            java.util.function.Consumer<android.widget.ImageView> artwork, Runnable play) {
        this.day = day; this.title = title; this.meta = meta;
        this.description = description; this.artwork = artwork; this.play = play;
    }
}
