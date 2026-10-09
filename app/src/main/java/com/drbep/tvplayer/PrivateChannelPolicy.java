package com.drbep.tvplayer;

import java.util.List;

/** Private viewing must not populate resume/history, even while parental access is unlocked. */
final class PrivateChannelPolicy {
    private static final java.util.regex.Pattern ADULT_LABEL = java.util.regex.Pattern.compile(".*(?:\\badult(?:o|a|os|as|s)?\\b|\\bxxx\\b|\\bporno?\\b|\\berotic[a-z]*\\b|\\+18|18\\+).*");
    static boolean adultLabel(String text) {
        String value = SettingsSearchIndex.normalize(text);
        return ADULT_LABEL.matcher(value).matches();
    }
    static boolean privateItem(ChannelItem item, OfflinePermissions permissions) {
        if (item == null) return false;
        if (item.isAdultVod || (permissions != null && (permissions.isProtectedItem(item) || permissions.protectedFilterKeys.contains("platform:" + item.platformId)))
                || adultLabel(item.group) || adultLabel(item.platformName) || adultLabel(item.name)) return true;
        if (item.customGroups != null) for (String group : item.customGroups) if (adultLabel(group)) return true;
        return false;
    }
    static boolean privateFilter(ChannelFilter filter, OfflinePermissions permissions) {
        return filter != null && ((permissions != null && permissions.isProtectedFilter(filter))
                || adultLabel(filter.label) || adultLabel(filter.groupName) || adultLabel(filter.key));
    }
    static boolean movistarIsm(String name) {
        String value = SettingsSearchIndex.normalize(name);
        return value.contains("movistar") && value.contains("ism");
    }
    static ChannelItem fallback(List<ChannelItem> channels, OfflinePermissions permissions) {
        ChannelItem first = null;
        for (ChannelItem item : channels) {
            if (item == null || item.isVod || privateItem(item, permissions) || item.playUrl.isEmpty()) continue;
            if (first == null) first = item;
            if (movistarIsm(item.platformName) || movistarIsm(item.group)) return item;
        }
        return first;
    }
}
