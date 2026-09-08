package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class MultiViewChannelSelector {
    static final class PlatformGroup {
        final String key;
        final int platformId;
        final String name;
        final String logoUrl;
        final List<ChannelItem> channels;

        PlatformGroup(String key, int platformId, String name, String logoUrl, List<ChannelItem> channels) {
            this.key = key == null ? "" : key;
            this.platformId = platformId;
            this.name = name == null ? "" : name;
            this.logoUrl = logoUrl == null ? "" : logoUrl;
            this.channels = channels == null ? new ArrayList<>() : new ArrayList<>(channels);
        }
    }

    private static final class MutablePlatformGroup {
        final String key;
        final int platformId;
        final String name;
        String logoUrl;
        final List<ChannelItem> channels = new ArrayList<>();

        MutablePlatformGroup(String key, ChannelItem first) {
            this.key = key;
            this.platformId = first == null ? 0 : first.platformId;
            this.name = first == null || first.platformName == null ? "" : first.platformName.trim();
            this.logoUrl = first == null || first.platformLogoUrl == null ? "" : first.platformLogoUrl.trim();
        }

        PlatformGroup freeze() {
            return new PlatformGroup(key, platformId, name, logoUrl, channels);
        }
    }

    private MultiViewChannelSelector() {
    }

    static List<ChannelItem> selectable(
            List<ChannelItem> catalog,
            List<ChannelItem> selected,
            int targetSlot
    ) {
        List<ChannelItem> result = new ArrayList<>();
        Set<String> used = new HashSet<>();
        if (selected != null) {
            for (int index = 0; index < selected.size(); index++) {
                if (index == targetSlot) {
                    continue;
                }
                ChannelItem item = selected.get(index);
                if (item != null && item.id != null && !item.id.trim().isEmpty()) {
                    used.add(item.id);
                }
            }
        }
        if (catalog == null) {
            return result;
        }
        for (ChannelItem item : catalog) {
            if (item == null || item.isVod || item.id == null || item.id.trim().isEmpty() || !used.add(item.id)) {
                continue;
            }
            result.add(item);
        }
        return result;
    }

    static List<PlatformGroup> groupByPlatform(List<ChannelItem> channels) {
        Map<String, MutablePlatformGroup> grouped = new LinkedHashMap<>();
        if (channels != null) {
            for (ChannelItem item : channels) {
                if (item == null) {
                    continue;
                }
                String key = platformKey(item);
                MutablePlatformGroup group = grouped.get(key);
                if (group == null) {
                    group = new MutablePlatformGroup(key, item);
                    grouped.put(key, group);
                }
                if (group.logoUrl.isEmpty() && item.platformLogoUrl != null && !item.platformLogoUrl.trim().isEmpty()) {
                    group.logoUrl = item.platformLogoUrl.trim();
                }
                group.channels.add(item);
            }
        }
        List<PlatformGroup> result = new ArrayList<>();
        for (MutablePlatformGroup group : grouped.values()) {
            result.add(group.freeze());
        }
        return result;
    }

    private static String platformKey(ChannelItem item) {
        if (item.platformId > 0) {
            return "id:" + item.platformId;
        }
        String name = item.platformName == null ? "" : item.platformName.trim().toLowerCase(Locale.ROOT);
        return name.isEmpty() ? "unassigned" : "name:" + name;
    }
}
