package com.drbep.tvplayer;

import java.util.*;
import java.util.function.Predicate;

/** Per-build selection only: no retained catalogue or permission decisions. */
final class FavoriteQuickChannels {
    static List<ChannelItem> build(List<ChannelItem> channels, Set<String> favorites,
            List<String> orderedIds, Predicate<ChannelItem> hidden) {
        List<ChannelItem> result = new ArrayList<>();
        if (favorites.isEmpty()) return result;
        Map<String, ChannelItem> byId = new HashMap<>();
        if (!orderedIds.isEmpty()) {
            for (ChannelItem item : channels) {
                if (item != null && item.id != null && !item.id.trim().isEmpty()
                        && favorites.contains(item.id)) byId.putIfAbsent(item.id, item);
            }
        }
        Set<String> added = new HashSet<>();
        for (String id : orderedIds) {
            ChannelItem item = byId.get(id);
            if (item != null && favorites.contains(id) && !hidden.test(item) && added.add(id)) result.add(item);
        }
        for (ChannelItem item : channels) {
            if (item != null && item.id != null && favorites.contains(item.id)
                    && !hidden.test(item) && added.add(item.id)) result.add(item);
        }
        return result;
    }
    private FavoriteQuickChannels() {}
}
