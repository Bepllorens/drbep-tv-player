package com.drbep.tvplayer;

import java.util.*;

/** Categories are grants, while channel membership comes from the server-filtered catalog. */
final class LiveHomePolicy {
    static boolean isLiveChannel(ChannelItem item) {
        return item != null && !item.isVod && !DaznEventPolicy.isCompetitionLauncher(item)
                && (item.playUrl == null || !item.playUrl.startsWith("drbep-action:"));
    }
    static String normalized(String s) { return s == null ? "" : s.trim().toLowerCase(Locale.ROOT); }
    static boolean platformAllowed(OfflinePermissions p, int id) {
        return p != null && p.liveEnabled && p.liveAccessMetadataKnown && p.allowedPlatformIds.contains(id);
    }
    static boolean groupAllowed(OfflinePermissions p, String name) {
        return p != null && p.liveEnabled && p.liveAccessMetadataKnown && p.allowedLiveGroups.contains(normalized(name));
    }
    static boolean visibleInHome(ChannelItem c, OfflinePermissions p) {
        if (!isLiveChannel(c) || p == null || !p.liveEnabled) return false;
        // Non-admin snapshots already enforce channel-specific grants and denials.
        if (!p.admin) return true;
        if (platformAllowed(p, c.platformId)) return true;
        for (String group : groups(c)) if (groupAllowed(p, group)) return true;
        return false;
    }
    static List<String> groups(ChannelItem c) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (c.group != null && !c.group.trim().isEmpty()) names.add(c.group);
        if (c.customGroups != null) names.addAll(c.customGroups);
        return new ArrayList<>(names);
    }
    static boolean matches(ChannelItem c, String key) {
        if ("ALL".equals(key)) return true;
        if (("live:" + c.platformId).equals(key)) return true;
        for (String group : groups(c)) if (("group:" + normalized(group)).equals(key)) return true;
        return false;
    }
    static List<StartupHomeHubUiModel.Provider> categories(List<ChannelItem> visible, OfflinePermissions p) {
        LinkedHashMap<String, StartupHomeHubUiModel.Provider> result = new LinkedHashMap<>();
        if (p == null || !p.liveEnabled || visible.isEmpty()) return new ArrayList<>();
        result.put("ALL", new StartupHomeHubUiModel.Provider("ALL", "Todos", ""));
        for (ChannelItem c : visible) {
            if (!visibleInHome(c, p)) continue;
            if (platformAllowed(p, c.platformId) && !normalized(c.platformName).isEmpty()) {
                String key = "live:" + c.platformId;
                result.putIfAbsent(key, new StartupHomeHubUiModel.Provider(key, c.platformName, c.platformLogoUrl));
            }
            for (String group : groups(c)) {
                if (normalized(group).isEmpty() || !groupAllowed(p, group)) continue;
                String key = "group:" + normalized(group);
                String logo = c.customGroupLogos == null ? "" : c.customGroupLogos.get(normalized(group));
                result.putIfAbsent(key, new StartupHomeHubUiModel.Provider(key, group, logo));
            }
        }
        return new ArrayList<>(result.values());
    }
}
