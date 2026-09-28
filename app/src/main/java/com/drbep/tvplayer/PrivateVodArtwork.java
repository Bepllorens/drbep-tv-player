package com.drbep.tvplayer;

import android.content.Context;
import com.bumptech.glide.load.model.GlideUrl;

/** Reuses the catalog's authenticated artwork request without putting credentials in intents. */
public final class PrivateVodArtwork {
    private PrivateVodArtwork() {}
    public static GlideUrl netflix(Context context, String apiBase, String query) {
        if (apiBase == null || apiBase.isEmpty() || query == null || query.isEmpty()) return null;
        CatalogRepository catalog = new CatalogRepository(apiBase, new CatalogSnapshotStore(context), true);
        return catalog.privateVodPoster("netflix", query);
    }
}
