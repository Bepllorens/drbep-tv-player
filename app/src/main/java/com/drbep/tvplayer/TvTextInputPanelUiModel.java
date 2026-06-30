package com.drbep.tvplayer;

import java.util.List;

public final class TvTextInputPanelUiModel {
    public interface SubmitAction {
        void submit(List<String> values);
    }

    public final String title;
    public final String message;
    public final String positiveLabel;
    public final String negativeLabel;
    public final String neutralLabel;
    public final List<TvTextInputFieldUiModel> fields;
    public final SubmitAction onSubmit;
    public final Runnable onCancel;
    public final Runnable onNeutral;

    public TvTextInputPanelUiModel(String title, String message, String positiveLabel, String negativeLabel, String neutralLabel, List<TvTextInputFieldUiModel> fields, SubmitAction onSubmit, Runnable onCancel, Runnable onNeutral) {
        this.title = title == null ? "" : title;
        this.message = message == null ? "" : message;
        this.positiveLabel = positiveLabel == null ? "" : positiveLabel;
        this.negativeLabel = negativeLabel == null ? "" : negativeLabel;
        this.neutralLabel = neutralLabel == null ? "" : neutralLabel;
        this.fields = fields;
        this.onSubmit = onSubmit;
        this.onCancel = onCancel;
        this.onNeutral = onNeutral;
    }
}
