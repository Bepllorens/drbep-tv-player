package com.drbep.tvplayer;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

/** Generated Glide integration without manifest reflection at application startup. */
@GlideModule
public final class DrbepGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(android.content.Context context, com.bumptech.glide.GlideBuilder builder) {
        android.app.ActivityManager am = (android.app.ActivityManager) context.getSystemService(android.content.Context.ACTIVITY_SERVICE);
        boolean low = am != null && am.isLowRamDevice();
        builder.setDiskCache(new com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory(context, 128L * 1024 * 1024));
        builder.setMemoryCache(new com.bumptech.glide.load.engine.cache.LruResourceCache((low ? 8L : 12L) * 1024 * 1024));
        builder.setBitmapPool(new com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool((low ? 4L : 8L) * 1024 * 1024));
        builder.setArrayPool(new com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool(2 * 1024 * 1024));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
