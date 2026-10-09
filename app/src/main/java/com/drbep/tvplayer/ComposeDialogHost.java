package com.drbep.tvplayer;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

public final class ComposeDialogHost {
    public interface DismissHandler {
        void onDismiss();
    }

    public interface CancelHandler {
        void onCancel();
    }

    private static final java.util.List<java.lang.ref.WeakReference<Dialog>> OPEN = new java.util.ArrayList<>();

    public static void dismissAll(Context context) {
        java.util.List<java.lang.ref.WeakReference<Dialog>> snapshot = new java.util.ArrayList<>(OPEN);
        for (java.lang.ref.WeakReference<Dialog> reference : snapshot) {
            Dialog dialog = reference.get();
            if (dialog != null && dialog.getContext() == context) dialog.dismiss();
            else if (dialog != null && dialog.getOwnerActivity() == context) dialog.dismiss();
        }
    }

    private ComposeDialogHost() {
    }

    public static Dialog showFullscreen(Context context, View content, DismissHandler onDismiss) {
        return showFullscreen(context, content, null, onDismiss);
    }

    public static Dialog showFullscreen(Context context, View content, CancelHandler onCancel, DismissHandler onDismiss) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        content.setFocusable(true);
        content.setFocusableInTouchMode(true);
        dialog.setContentView(content);
        if (onCancel != null) {
            dialog.setOnCancelListener(d -> onCancel.onCancel());
        }
        if (context instanceof android.app.Activity) dialog.setOwnerActivity((android.app.Activity) context);
        java.lang.ref.WeakReference<Dialog> reference = new java.lang.ref.WeakReference<>(dialog);
        OPEN.add(reference);
        dialog.setOnDismissListener(d -> {
            OPEN.remove(reference);
            if (onDismiss != null) onDismiss.onDismiss();
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setDimAmount(0f);
            // Let the Compose gradient reveal playback instead of the theme's black window.
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        content.post(content::requestFocus);
        return dialog;
    }
}
