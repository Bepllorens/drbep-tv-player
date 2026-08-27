package com.drbep.tvplayer;

import androidx.compose.ui.platform.ComposeView;

final class OfflineComposeSurfaceRenderer {
    void bindTimeshift(ComposeView view, TimeshiftBarUiModel model) {
        TimeshiftBarComposeBinder.bind(view, model);
    }

    void bindTouchControls(ComposeView view, TouchControlsBarUiModel model, TouchControlsArtworkBinder artworkBinder) {
        TouchControlsComposeBinder.bind(view, model, artworkBinder);
    }

    void bindOverlayChannelList(ComposeView view, OverlayChannelListUiModel model, OverlayChannelImageBinder imageBinder) {
        OverlayChannelListComposeBinder.bind(view, model, imageBinder);
    }

    void bindOverlayControls(ComposeView view, OverlayControlsUiModel model) {
        OverlayControlsComposeBinder.bind(view, model);
    }

    void bindOverlayNowPlaying(ComposeView view, ChannelOverlayUi.NowPlayingModel model, ZapLogoBinder logoBinder) {
        OverlayNowPlayingComposeBinder.bind(view, model, logoBinder);
    }

    void bindChannelOverlaySurface(
            ComposeView nowPlayingView,
            ComposeView controlsView,
            ComposeView listView,
            ChannelOverlaySurfaceUiModel model,
            OverlayChannelImageBinder imageBinder,
            ZapLogoBinder contextLogoBinder
    ) {
        if (model == null) {
            return;
        }
        bindOverlayNowPlaying(nowPlayingView, model.nowPlaying, contextLogoBinder);
        bindOverlayControls(controlsView, model.controls);
        bindOverlayChannelList(listView, model.channelList, imageBinder);
    }

    void bindRecordingsSurface(ComposeView view, RecordingsSurfaceUiModel model, RecordingPosterBinder posterBinder) {
        RecordingsSurfaceComposeBinder.bind(view, model, posterBinder);
    }
}
