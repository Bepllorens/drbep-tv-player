package com.drbep.tvplayer;

import android.app.Activity;
import android.content.Context;
import android.view.Display;
import android.view.WindowManager;
import java.util.Locale;

/** Window-scoped preference: no system settings, resolution changes or frame interpolation. */
final class VodDisplayRate {
    private final Activity activity;
    private Integer savedMode;
    private float savedRate;
    private float attemptedFps;

    VodDisplayRate(Context context) { activity = context instanceof Activity ? (Activity) context : null; }

    void update(boolean enabled, float fps) {
        if (!enabled) { restore(); return; }
        if (activity == null || android.os.Build.VERSION.SDK_INT < 23 || fps <= 0 || !Float.isFinite(fps)) return;
        if (Math.abs(attemptedFps - fps) < 0.012f) return;
        attemptedFps = fps; // Do not repeatedly request a mode the system has refused.
        Display display = activity.getWindowManager().getDefaultDisplay();
        Display.Mode current = display.getMode();
        java.util.List<Display.Mode> modes = new java.util.ArrayList<>();
        for (Display.Mode mode : display.getSupportedModes()) {
            if (mode.getPhysicalWidth() == current.getPhysicalWidth()
                    && mode.getPhysicalHeight() == current.getPhysicalHeight()) modes.add(mode);
        }
        float[] rates = new float[modes.size()];
        for (int i = 0; i < rates.length; i++) rates[i] = modes.get(i).getRefreshRate();
        int choice = VodFrameRatePolicy.choose(fps, current.getRefreshRate(), rates);
        if (choice < 0) return;
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        if (savedMode == null) { savedMode = params.preferredDisplayModeId; savedRate = params.preferredRefreshRate; }
        params.preferredDisplayModeId = modes.get(choice).getModeId();
        activity.getWindow().setAttributes(params);
    }

    void restore() {
        attemptedFps = 0;
        if (activity == null || savedMode == null) return;
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.preferredDisplayModeId = savedMode;
        params.preferredRefreshRate = savedRate;
        activity.getWindow().setAttributes(params);
        savedMode = null;
    }

    String label(float fps) {
        String source = fps > 0 && Float.isFinite(fps) ? String.format(Locale.getDefault(), "%.3f fps", fps) : "fps no informados";
        if (activity == null) return source;
        float hz = activity.getWindowManager().getDefaultDisplay().getRefreshRate();
        return source + String.format(Locale.getDefault(), " · Salida: %.3f Hz", hz);
    }
}
