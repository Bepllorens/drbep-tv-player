package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class GlobalSearchUiFactory {
    interface Host {
        String text(int resId);
        String displayName(ChannelItem item);
        String recordingTitle(RecordingsRepository.RecordingItem item);
        String protectedTitle(ChannelItem item, String title);
        String protectedMeta(ChannelItem item, String meta);
        String protectedBadge(ChannelItem item, String fallback);
        String filterLabel(int filter);
        int[] filters();
        int currentFilter();
        boolean recordingsDisabled();
        boolean isHeader(MainActivity.GlobalSearchResult result);
        boolean isHistory(MainActivity.GlobalSearchResult result);
        void applyFilter(int filter);
        void applyQuery(String query);
        void rememberQuery(String query);
        void openResult(MainActivity.GlobalSearchResult result);
        void openActions(MainActivity.GlobalSearchResult result);
    }

    private GlobalSearchUiFactory() {
    }

    static GlobalSearchListUiModel build(String query, List<MainActivity.GlobalSearchResult> results, Host host) {
        if (host == null) {
            return new GlobalSearchListUiModel("", "", query, new ArrayList<>(), null, new ArrayList<>());
        }
        List<GlobalSearchRowUiModel> rows = new ArrayList<>();
        if (results != null) {
            java.util.Map<String, Integer> occurrences = new java.util.HashMap<>();
            for (MainActivity.GlobalSearchResult result : results) {
                GlobalSearchRowUiModel row = buildRow(query, result, host);
                String id = (result == null ? "" : result.searchSource) + "|" + identity(result);
                int occurrence = occurrences.getOrDefault(id, 0);
                occurrences.put(id, occurrence + 1);
                row.key = id + "#" + occurrence;
                rows.add(row);
            }
        }
        return new GlobalSearchListUiModel(
                host.text(R.string.title_global_search),
                host.text(R.string.global_search_hint),
                query,
                buildFilters(host),
                host::applyQuery,
                rows
        );
    }

    static String identity(MainActivity.GlobalSearchResult result) {
        if (result == null) return "empty";
        String prefix = result.type + "|";
        if (result.epgResult != null) {
            MainActivity.EpgSearchResult epg = result.epgResult;
            return prefix + (epg.channel == null ? "" : epg.channel.id) + "|" + epg.program.startTime + "|" + epg.program.endTime;
        }
        if (result.recording != null) return prefix + result.recordingBasePath + "|" + result.recording.id;
        if (result.channel != null) return prefix + result.channel.id;
        return prefix + result.title;
    }

    private static List<GlobalSearchFilterUiModel> buildFilters(Host host) {
        List<GlobalSearchFilterUiModel> filters = new ArrayList<>();
        int[] filterIds = host.filters();
        if (filterIds == null) {
            return filters;
        }
        for (int filter : filterIds) {
            if (filter == 5 && host.recordingsDisabled()) {
                continue;
            }
            filters.add(new GlobalSearchFilterUiModel(host.filterLabel(filter), filter == host.currentFilter(), () -> host.applyFilter(filter)));
        }
        return filters;
    }

    private static GlobalSearchRowUiModel buildRow(String query, MainActivity.GlobalSearchResult result, Host host) {
        boolean header = host.isHeader(result);
        int imageKind = GlobalSearchRowUiModel.IMAGE_NONE;
        String imageUrl = "";
        String imageName = "";
        if (!header && result != null) {
            if (result.channel != null) {
                imageKind = GlobalSearchRowUiModel.IMAGE_CHANNEL;
                imageUrl = result.channel.logoUrl;
                imageName = host.displayName(result.channel);
            } else if (result.epgResult != null) {
                imageKind = GlobalSearchRowUiModel.IMAGE_PROGRAM;
                EpgRepository.EpgProgram program = result.epgResult.program;
                ChannelItem channel = result.epgResult.channel;
                imageUrl = ProgramArtworkResolver.resolve(program, channel);
                imageName = channel == null ? "" : host.displayName(channel);
            } else if (result.recording != null) {
                imageKind = GlobalSearchRowUiModel.IMAGE_RECORDING;
                imageUrl = result.recording.poster;
                imageName = host.recordingTitle(result.recording);
            }
        }
        return new GlobalSearchRowUiModel(
                result == null ? "" : host.protectedTitle(result.channel, result.title),
                header || result == null ? "" : host.protectedMeta(result.channel, result.meta),
                header || result == null ? "" : host.protectedBadge(result.channel, result.badge),
                header,
                imageKind,
                imageUrl,
                imageName,
                header ? null : () -> {
                    if (result == null) {
                        return;
                    }
                    if (host.isHistory(result)) {
                        host.applyQuery(result.title);
                        return;
                    }
                    host.rememberQuery(query);
                    host.openResult(result);
                },
                header ? null : () -> {
                    if (result != null) {
                        host.openActions(result);
                    }
                }
        );
    }
}
