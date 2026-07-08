package com.drbep.tvplayer;

public final class StartupLoadingUiModel {
    final String title;
    final String step;
    final String detail;

    StartupLoadingUiModel(String title, String step, String detail) {
        this.title = title == null ? "" : title;
        this.step = step == null ? "" : step;
        this.detail = detail == null ? "" : detail;
    }
}
