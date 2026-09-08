package com.drbep.tvplayer;

import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

/** Home-only image diagnostics: never log model, URL, headers or exception text. */
final class HomePosterLoader {
    private HomePosterLoader() {}

    static void bind(ImageView image, Object model, int width, int height) {
        final long started = SystemClock.elapsedRealtime();
        image.setVisibility(View.VISIBLE);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Glide.with(image.getContext()).load(model)
                .fitCenter().override(width, height)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.home_artwork_placeholder)
                .error(R.drawable.home_artwork_placeholder)
                .listener(new RequestListener<Drawable>() {
                    @Override public boolean onLoadFailed(GlideException error, Object ignoredModel,
                            Target<Drawable> target, boolean first) {
                        Log.println(Log.INFO, "HomeArtwork", "result=failed elapsed_ms=" + (SystemClock.elapsedRealtime() - started));
                        return false;
                    }
                    @Override public boolean onResourceReady(Drawable resource, Object ignoredModel,
                            Target<Drawable> target, DataSource source, boolean first) {
                        Log.println(Log.INFO, "HomeArtwork", "result=ready elapsed_ms=" + (SystemClock.elapsedRealtime() - started)
                                + " source=" + source.name());
                        return false;
                    }
                }).into(image);
    }
}
