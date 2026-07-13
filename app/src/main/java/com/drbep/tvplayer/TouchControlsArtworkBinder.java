package com.drbep.tvplayer;

import android.widget.ImageView;

public interface TouchControlsArtworkBinder {
    void bindLogo(ImageView imageView, String logoUrl, String channelName, int widthDp, int heightDp);

    void bindPoster(ImageView imageView, String posterUrl);
}
