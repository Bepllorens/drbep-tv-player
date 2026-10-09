package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A bounded home projection; never treats a missing category as all channels. */
final class LiveHomeFeed {
    static List<ChannelItem> select(List<ChannelItem> candidates, String group,
            OfflinePermissions permissions, int limit) {
        List<ChannelItem> result = new ArrayList<>();
        if (group == null || group.isEmpty() || "ALL".equals(group) || limit <= 0) return result;
        boolean available = false;
        for (StartupHomeHubUiModel.Provider category : LiveHomePolicy.categories(candidates, permissions)) {
            if (group.equals(category.key)) { available = true; break; }
        }
        if (!available) return result;
        Set<String> seen = new LinkedHashSet<>();
        for (ChannelItem item : candidates) {
            if (!LiveHomePolicy.visibleInHome(item, permissions) || !LiveHomePolicy.matches(item, group)
                    || !seen.add(item.id)) continue;
            result.add(item);
            if (result.size() >= limit) break;
        }
        return result;
    }
}
