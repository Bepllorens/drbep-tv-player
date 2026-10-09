package com.drbep.tvplayer;

import java.util.*;

/** Applies a local preference only to currently authorized categories. */
final class LiveCategoryOrder {
    static List<StartupHomeHubUiModel.Provider> apply(List<StartupHomeHubUiModel.Provider> available, List<String> saved) {
        Map<String, StartupHomeHubUiModel.Provider> remaining = new LinkedHashMap<>();
        for (StartupHomeHubUiModel.Provider item : available) remaining.put(item.key, item);
        List<StartupHomeHubUiModel.Provider> result = new ArrayList<>();
        StartupHomeHubUiModel.Provider all = remaining.remove("ALL");
        if (all != null) result.add(all);
        for (String key : saved) {
            StartupHomeHubUiModel.Provider item = remaining.remove(key);
            if (item != null) result.add(item);
        }
        result.addAll(remaining.values());
        return result;
    }
    static List<StartupHomeHubUiModel.Provider> move(List<StartupHomeHubUiModel.Provider> items, String key, int delta) {
        List<StartupHomeHubUiModel.Provider> result = new ArrayList<>(items);
        if ("ALL".equals(key)) return result;
        for (int i = 0; i < result.size(); i++) {
            if (!key.equals(result.get(i).key)) continue;
            int next = i + delta;
            if (next >= 0 && next < result.size() && !"ALL".equals(result.get(next).key)) Collections.swap(result, i, next);
            break;
        }
        return result;
    }
}
