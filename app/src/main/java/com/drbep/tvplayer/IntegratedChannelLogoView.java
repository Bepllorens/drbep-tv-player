package com.drbep.tvplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import androidx.appcompat.widget.AppCompatImageView;
import java.util.WeakHashMap;

/** Transparent channel art; only dark marks get a soft contrast halo, never a plate. */
final class IntegratedChannelLogoView extends AppCompatImageView {
    private static final WeakHashMap<Drawable.ConstantState, Boolean> DARK = new WeakHashMap<>();

    IntegratedChannelLogoView(Context context) { super(context); }

    @Override public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        boolean dark = drawable != null && needsContrast(drawable);
        if (!dark) { setBackground(null); return; }
        GradientDrawable halo = new GradientDrawable();
        halo.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        halo.setColors(new int[]{0xFF8795A2, 0x008795A2});
        halo.setGradientRadius(24f * getResources().getDisplayMetrics().density);
        setBackground(halo);
    }

    private static boolean needsContrast(Drawable drawable) {
        Drawable.ConstantState key = drawable.getConstantState();
        Boolean cached = key == null ? null : DARK.get(key);
        if (cached != null) return cached;
        // Tiny, one-off sample per cached drawable, not per frame or focus change.
        Bitmap sample = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        Rect bounds = new Rect(drawable.getBounds());
        int visible = 0, dark = 0;
        try {
            drawable.setBounds(0, 0, 16, 16);
            drawable.draw(new Canvas(sample));
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                int pixel = sample.getPixel(x, y);
                if (Color.alpha(pixel) < 96) continue;
                visible++;
                if (Color.red(pixel) * .2126 + Color.green(pixel) * .7152 + Color.blue(pixel) * .0722 < 75) dark++;
            }
        } catch (RuntimeException unsupportedDrawable) {
            // Some hardware-backed drawables cannot be sampled on a software canvas.
            return false;
        } finally { drawable.setBounds(bounds); sample.recycle(); }
        boolean result = visible > 0 && dark > visible * .65;
        if (key != null) DARK.put(key, result);
        return result;
    }
}
