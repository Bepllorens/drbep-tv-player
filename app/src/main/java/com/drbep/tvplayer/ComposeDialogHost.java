package com.drbep.tvplayer;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

final class ComposeDialogHost {
    interface DismissHandler {
        void onDismiss();
    }

    interface CancelHandler {
        void onCancel();
    }

    private ComposeDialogHost() {
    }

    static Dialog showFullscreen(Context context, View content, DismissHandler onDismiss) {
        return showFullscreen(context, content, null, onDismiss);
    }

    static Dialog showFullscreen(Context context, View content, CancelHandler onCancel, DismissHandler onDismiss) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        content.setFocusable(true);
        content.setFocusableInTouchMode(true);
        dialog.setContentView(content);
        if (onCancel != null) {
            dialog.setOnCancelListener(d -> onCancel.onCancel());
        }
        if (onDismiss != null) {
            dialog.setOnDismissListener(d -> onDismiss.onDismiss());
        }
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setDimAmount(0f);
        }
        content.post(content::requestFocus);
        return dialog;
    }
}
