package com.drbep.tvplayer;

import android.widget.ImageView;

public interface StartupHomeHubArtworkBinder {
    default Runnable loadLibraryInfo(StartupHomeHubUiModel.ContinueCard card, java.util.function.Consumer<String[]> ready) {
        ready.accept(new String[]{card.description, card.facts});
        return () -> {};
    }

    void bindLogo(ImageView imageView, String logoUrl, String channelName, int widthDp, int heightDp);

    void bindPoster(ImageView imageView, String posterUrl, int widthDp, int heightDp);

    default void bindCard(ImageView image, String url, int width, int height, java.util.function.Consumer<Float> aspect) {
        bindPoster(image, url, width, height);
    }

    default void bindArtworkCard(ImageView image, StartupHomeHubUiModel.ContinueCard card, boolean portrait, int width, int height, java.util.function.Consumer<Float> aspect) {
        bindCard(image, card.imageUrl, width, height, aspect);
    }

    void bindLivePreview(ImageView imageView, String fallbackLogoUrl, String channelName, int widthDp, int heightDp);

    default void bindHero(ImageView image, java.util.List<StartupHomeHubUiModel.ContinueCard> cards, java.util.function.IntConsumer selected, int width, int height) {
        if (!cards.isEmpty()) { selected.accept(0); bindPoster(image, cards.get(0).imageUrl, width, height); }
    }

    default void bindSelectedLivePreview(ImageView image, StartupHomeHubUiModel.ContinueCard card, int width, int height) { }
    default void releaseLivePreview() { }
    default void onLiveGroupChanged() { }

    default void releaseArtwork(ImageView imageView) { }
}
