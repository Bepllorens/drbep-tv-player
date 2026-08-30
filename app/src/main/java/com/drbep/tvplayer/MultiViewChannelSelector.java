package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class MultiViewChannelSelector {
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
}
