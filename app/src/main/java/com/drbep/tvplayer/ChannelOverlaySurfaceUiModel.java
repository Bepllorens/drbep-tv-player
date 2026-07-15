package com.drbep.tvplayer;

public final class ChannelOverlaySurfaceUiModel {
    public final ChannelOverlayUi.NowPlayingModel nowPlaying;
    public final OverlayControlsUiModel controls;
    public final OverlayChannelListUiModel channelList;

    public ChannelOverlaySurfaceUiModel(
            ChannelOverlayUi.NowPlayingModel nowPlaying,
            OverlayControlsUiModel controls,
            OverlayChannelListUiModel channelList
    ) {
        this.nowPlaying = nowPlaying;
        this.controls = controls;
        this.channelList = channelList;
    }
}
