package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class TimelineScaleUiModel {
    public final int leadingWidthPx;
    public final List<TimelineScaleSlotUiModel> slots;

    public TimelineScaleUiModel(int leadingWidthPx, List<TimelineScaleSlotUiModel> slots) {
        this.leadingWidthPx = leadingWidthPx;
        this.slots = slots == null ? new ArrayList<>() : new ArrayList<>(slots);
    }
}
