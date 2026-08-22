package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class EpgGuideUiFactory {
    interface Host {
        String text(int resId);
        String displayName(ChannelItem item);
        String shortTime(String isoTime);
        String guideMeta(EpgRepository.EpgProgram program);
        void openProgramActions(ChannelItem channel, EpgRepository.EpgProgram program);
    }

    private EpgGuideUiFactory() {
    }

    static EpgSearchResultListUiModel buildSearchResults(String title, String subtitle, List<MainActivity.EpgSearchResult> results, Host host) {
        List<EpgSearchResultRowUiModel> rows = new ArrayList<>();
        if (host != null && results != null) {
            for (MainActivity.EpgSearchResult result : results) {
                if (result == null) {
                    continue;
                }
                ChannelItem channel = result.channel;
                EpgRepository.EpgProgram program = result.program;
                String programTitle = program == null || program.title == null || program.title.trim().isEmpty()
                        ? host.text(R.string.label_program_default)
                        : program.title.trim();
                String channelName = channel == null ? "" : host.displayName(channel);
                String time = program == null ? "" : host.shortTime(program.startTime) + " - " + host.shortTime(program.endTime);
                String meta = (channelName + "  ·  " + time).trim();
                boolean live = program != null && program.progress >= 0;
                String badge = host.text(live ? R.string.epg_search_badge_live : R.string.epg_search_badge_next);
                int badgeColor = live ? 0xFF276B49 : OfflineTvTheme.cardArgb();
                String imageUrl = ProgramArtworkResolver.resolve(program, channel);
                rows.add(new EpgSearchResultRowUiModel(
                        programTitle,
                        meta,
                        badge,
                        badgeColor,
                        imageUrl,
                        () -> host.openProgramActions(channel, program)
                ));
            }
        }
        return new EpgSearchResultListUiModel(title, subtitle, rows);
    }

    static MiniGuideUiModel buildMiniGuide(ChannelItem channel, List<EpgRepository.EpgProgram> items, String title, String subtitle, Host host) {
        List<MiniGuideProgramRowUiModel> rows = new ArrayList<>();
        if (host != null && items != null) {
            for (int i = 0; i < items.size(); i++) {
                EpgRepository.EpgProgram program = items.get(i);
                if (program == null) {
                    continue;
                }
                String badge;
                int badgeColor;
                if (program.progress >= 0) {
                    badge = host.text(R.string.guide_program_now);
                    badgeColor = 0xAA266D3E;
                } else if (i == 1) {
                    badge = host.text(R.string.guide_program_next);
                    badgeColor = 0xAA405C86;
                } else {
                    badge = host.text(R.string.guide_program_later);
                    badgeColor = 0xAA4B5361;
                }
                EpgRepository.EpgProgram rowProgram = program;
                rows.add(new MiniGuideProgramRowUiModel(
                        host.shortTime(program.startTime) + " - " + host.shortTime(program.endTime),
                        badge,
                        badgeColor,
                        program.title == null || program.title.trim().isEmpty() ? host.text(R.string.label_program_default) : program.title,
                        program.progress >= 0 ? Math.min(100, Math.max(0, program.progress)) : -1,
                        host.guideMeta(program),
                        () -> host.openProgramActions(channel, rowProgram)
                ));
            }
        }
        return new MiniGuideUiModel(title, subtitle, rows);
    }
}
