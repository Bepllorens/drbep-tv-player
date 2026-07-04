package com.drbep.tvplayer;

import java.util.List;

final class ZapBannerUiFactory {
    interface Host {
        String text(int resId);
        String text(int resId, Object... args);
        String displayName(ChannelItem item);
        String channelBadge(ChannelItem item);
        String programMeta(ChannelItem item, EpgRepository.EpgProgram program);
        String playbackQuality(PlayerController.PlaybackDiagnostics diagnostics);
        String durationShort(long durationMs);
        String shortTime(String isoTime);
        long parseIsoMillis(String isoTime);
        long nowMs();
    }

    private ZapBannerUiFactory() {
    }

    static ZapBannerUiModel build(
            ChannelItem channelItem,
            EpgRepository.EpgProgramPair pair,
            PlayerController.PlaybackDiagnostics diagnostics,
            List<ZapActionItem> actions,
            Host host
    ) {
        if (channelItem == null || host == null) {
            return new ZapBannerUiModel("", "", "", "", false, "", "", "", false, "", 0, false, "", actions);
        }
        EpgRepository.EpgProgram currentProgram = pair == null ? null : pair.current;
        EpgRepository.EpgProgram nextProgram = pair == null ? null : pair.next;

        String currentTitle = currentProgram != null && currentProgram.title != null && !currentProgram.title.trim().isEmpty()
                ? currentProgram.title.trim()
                : (channelItem.nowProgram == null ? "" : channelItem.nowProgram.trim());
        String nextTitle = nextProgram != null && nextProgram.title != null && !nextProgram.title.trim().isEmpty()
                ? nextProgram.title.trim()
                : (channelItem.nextProgram == null ? "" : channelItem.nextProgram.trim());

        String qualityLabel = host.playbackQuality(diagnostics);
        boolean qualityVisible = !qualityLabel.trim().isEmpty()
                || (diagnostics != null && diagnostics.playbackState != null && !"IDLE".equalsIgnoreCase(diagnostics.playbackState));
        String qualityText = !qualityLabel.trim().isEmpty()
                ? host.text(R.string.overlay_playback_quality, qualityLabel)
                : host.text(R.string.overlay_playback_quality_detecting);

        int progress = currentProgram == null ? 0 : Math.max(0, Math.min(100, currentProgram.progress));
        boolean progressVisible = currentProgram != null && currentProgram.progress >= 0;
        long endMs = currentProgram == null ? 0L : host.parseIsoMillis(currentProgram.endTime);
        long nowMs = host.nowMs();
        String remainingText = (progressVisible && endMs > nowMs)
                ? host.text(R.string.zap_banner_remaining, host.durationShort(endMs - nowMs))
                : "";
        String endTimeText = currentProgram == null ? "" : host.shortTime(currentProgram.endTime);

        return new ZapBannerUiModel(
                channelItem.logoUrl,
                host.channelBadge(channelItem),
                host.displayName(channelItem),
                qualityText,
                qualityVisible,
                currentTitle.isEmpty() ? host.text(R.string.zap_banner_epg_missing) : currentTitle,
                host.programMeta(channelItem, currentProgram),
                nextTitle.isEmpty() ? "" : host.text(R.string.zap_banner_next_prefix) + ": " + nextTitle,
                !nextTitle.isEmpty(),
                remainingText,
                progress,
                progressVisible,
                endTimeText,
                actions
        );
    }
}
