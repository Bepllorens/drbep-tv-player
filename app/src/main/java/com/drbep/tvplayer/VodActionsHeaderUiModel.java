package com.drbep.tvplayer;

public final class VodActionsHeaderUiModel {
    public final String title;
    public final String meta;

    public VodActionsHeaderUiModel(String title, String meta) {
        this.title = title == null ? "" : title;
        this.meta = meta == null ? "" : meta;
    }
}
