package com.drbep.tvplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import androidx.appcompat.widget.AppCompatImageView;
import java.util.WeakHashMap;

/** Transparent channel art; only dark marks get a soft contrast halo, never a plate. */
final class IntegratedChannelLogoView extends AppCompatImageView {
    private static final WeakHashMap<Drawable.ConstantState, Boolean> DARK = new WeakHashMap<>();

    private static final ThreadLocal<ContrastSample> SAMPLES = new ThreadLocal<>();
    private GradientDrawable contrastHalo;

    // One tiny scratch image per calling thread, never a catalogue of logo bitmaps.
    private static final class ContrastSample {
        final Bitmap bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        final int[] pixels = new int[16 * 16];
    }

    IntegratedChannelLogoView(Context context) { super(context); }

    @Override public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        boolean dark;
        android.os.Trace.beginSection("DRBEP.logoContrast");
        try { dark = drawable != null && needsContrast(drawable); }
        finally { android.os.Trace.endSection(); }
        if (!dark) { if (getBackground() != null) setBackground(null); return; }
        if (contrastHalo == null) {
            contrastHalo = new GradientDrawable();
            contrastHalo.setGradientType(GradientDrawable.RADIAL_GRADIENT);
            contrastHalo.setColors(new int[]{0xFF8795A2, 0x008795A2});
            contrastHalo.setGradientRadius(24f * getResources().getDisplayMetrics().density);
        }
        if (getBackground() != contrastHalo) setBackground(contrastHalo);
    }

    @Override protected void onDraw(Canvas canvas) {
        android.os.Trace.beginSection("DRBEP.logoDraw");
        try { super.onDraw(canvas); } finally { android.os.Trace.endSection(); }
    }
    @Override protected void onMeasure(int width, int height) {
        android.os.Trace.beginSection("DRBEP.logoMeasure");
        try { super.onMeasure(width, height); } finally { android.os.Trace.endSection(); }
    }

    private static boolean needsContrast(Drawable drawable) {
        Drawable.ConstantState key = drawable.getConstantState();
        Boolean cached = key == null ? null : DARK.get(key);
        if (cached != null) return cached;
        // Tiny, one-off sample per cached drawable, not per frame or focus change.
        ContrastSample sample = SAMPLES.get();
        if (sample == null) { sample = new ContrastSample(); SAMPLES.set(sample); }
        Rect bounds = new Rect(drawable.getBounds());
        try {
            // A reused surface must start transparent, including after failed draws.
            sample.bitmap.eraseColor(android.graphics.Color.TRANSPARENT);
            drawable.setBounds(0, 0, 16, 16);
            drawable.draw(sample.canvas);
            // One native read instead of 256 getPixel calls per uncached drawable.
            sample.bitmap.getPixels(sample.pixels, 0, 16, 0, 0, 16, 16);
        } catch (RuntimeException unsupportedDrawable) {
            // Some hardware-backed drawables cannot be sampled on a software canvas.
            return false;
        } finally { drawable.setBounds(bounds); }
        boolean result = LogoContrast.needsHalo(sample.pixels);
        if (key != null) DARK.put(key, result);
        return result;
    }
}
