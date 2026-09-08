package com.drbep.tvplayer;

import android.widget.ImageView;

public interface StartupHomeHubArtworkBinder {
    void bindLogo(ImageView imageView, String logoUrl, String channelName, int widthDp, int heightDp);

    void bindPoster(ImageView imageView, String posterUrl, int widthDp, int heightDp);

    void bindLivePreview(ImageView imageView, String fallbackLogoUrl, String channelName, int widthDp, int heightDp);

    default void releaseArtwork(ImageView imageView) { }
}
