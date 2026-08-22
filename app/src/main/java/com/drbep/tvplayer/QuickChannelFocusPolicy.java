package com.drbep.tvplayer;

import java.util.List;

final class QuickChannelFocusPolicy {
    private QuickChannelFocusPolicy() {
    }

    static int topActionIndex(List<ZapActionItem> actions) {
        if (actions == null) {
            return -1;
        }
        for (int i = 0; i < actions.size(); i++) {
            ZapActionItem action = actions.get(i);
            if (isEnabled(action) && (action.highlighted || action.selected)) {
                return i;
            }
        }
        return firstEnabledActionIndex(actions);
    }

    static int bottomActionIndex(List<ZapActionItem> actions) {
        if (actions == null) {
            return -1;
        }
        // Paged VOD panels consistently expose Previous and Next as the first
        // two actions. Prefer Next at the lower edge, falling back to Previous
        // on the final page.
        if (actions.size() > 1 && isEnabled(actions.get(1))) {
            return 1;
        }
        return firstEnabledActionIndex(actions);
    }

    private static int firstEnabledActionIndex(List<ZapActionItem> actions) {
        for (int i = 0; i < actions.size(); i++) {
            if (isEnabled(actions.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isEnabled(ZapActionItem action) {
        return action != null && action.enabled && action.onClick != null;
    }
}
