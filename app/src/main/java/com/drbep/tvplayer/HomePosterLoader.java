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

    static void bindPanoramic(ImageView image, java.util.List<Object> models,
            java.util.function.IntConsumer selected, int width, int height) {
        releaseHero(image);
        HeroRequest request = new HeroRequest(image, models, selected, width, height);
        image.setTag(R.id.startup_home_artwork, request);
        request.load(0);
    }

    static void releaseHero(ImageView image) {
        Object current = image.getTag(R.id.startup_home_artwork);
        image.setTag(R.id.startup_home_artwork, null);
        if (current instanceof CardRequest) ((CardRequest) current).clear();
        if (current instanceof HeroRequest) ((HeroRequest) current).clear();
    }

    static void bindCard(ImageView image, Object source, int width, int height, java.util.function.Consumer<Float> aspect) {
        releaseHero(image);
        CardRequest request = new CardRequest(image, source, width, height, aspect);
        image.setTag(R.id.startup_home_artwork, request);
        request.start();
    }

    private static final java.util.Map<String,Long> warmed = new java.util.LinkedHashMap<>();
    static void preloadCard(android.content.Context context, HomeTmdbArtwork.Source source, int width, int height) {
        String key=HomeTmdbArtwork.cacheKey(source);
        long now=SystemClock.elapsedRealtime();
        Long previous=warmed.get(key);
        if(previous!=null && now-previous<15*60_000L)return;
        if(warmed.size()>=32)warmed.remove(warmed.keySet().iterator().next());
        warmed.put(key,now);
        HomePublicArtwork.resolve(source, resolved -> cardRequest(context,resolved,width,height)
            .priority(com.bumptech.glide.Priority.LOW).preload(width,height));
    }
    private static com.bumptech.glide.RequestBuilder<Drawable> cardRequest(android.content.Context context, Object source, int width, int height) {
        return Glide.with(context).load(source).dontTransform().dontAnimate().override(width,height)
            .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565).disallowHardwareConfig()
            .downsample(com.bumptech.glide.load.resource.bitmap.DownsampleStrategy.AT_MOST)
            .diskCacheStrategy(DiskCacheStrategy.ALL);
    }

    private static final class CardRequest {
        final ImageView image;
        final Object original;
        final int width, height;
        final java.util.function.Consumer<Float> aspect;
        Runnable cancel;
        com.bumptech.glide.request.target.CustomTarget<Drawable> target;
        CardRequest(ImageView image, Object source, int width, int height, java.util.function.Consumer<Float> aspect) {
            this.image=image; original=source; this.width=width; this.height=height; this.aspect=aspect;
        }
        boolean active() { return image.getTag(R.id.startup_home_artwork)==this; }
        void clear() { if(cancel!=null)cancel.run(); image.setImageDrawable(null); if(target!=null)Glide.with(image).clear(target); target=null; }
        void start() { image.setImageResource(R.drawable.home_artwork_placeholder); cancel=HomePublicArtwork.resolve(original, this::load); }
        void load(Object source) {
            if(!active())return;
            if(target!=null) { image.setImageDrawable(null); Glide.with(image).clear(target); target=null; }
            target=new com.bumptech.glide.request.target.CustomTarget<Drawable>(width,height) {
                @Override public void onResourceReady(Drawable art, com.bumptech.glide.request.transition.Transition<? super Drawable> transition) {
                    if(!active())return;
                    float ratio=(float)Math.max(1,art.getIntrinsicWidth())/Math.max(1,art.getIntrinsicHeight());
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    image.setImageDrawable(art);
                    aspect.accept(ratio);
                    diagnostic("card",0,art.getIntrinsicWidth(),art.getIntrinsicHeight());
                }
                @Override public void onLoadFailed(Drawable ignored) {
                    if(!active())return;
                    if(source!=original && !(original instanceof HomeTmdbArtwork.Source)) image.post(() -> load(original));
                    else if (original instanceof HomeTmdbArtwork.Source && source!=((HomeTmdbArtwork.Source)original).original) image.post(() -> load(((HomeTmdbArtwork.Source)original).original));
                    else image.setImageResource(R.drawable.home_artwork_placeholder);
                }
                @Override public void onLoadCleared(Drawable ignored) { }
            };
            cardRequest(image.getContext(), source, width, height)
                .listener(new RequestListener<Drawable>() {
                    @Override public boolean onLoadFailed(GlideException e,Object m,Target<Drawable> t,boolean first){return false;}
                    @Override public boolean onResourceReady(Drawable d,Object m,Target<Drawable> t,DataSource cache,boolean first){
                        Log.println(Log.INFO,"HomeArtwork","card_cache="+cache.name());return false;
                    }
                }).into(target);
        }
    }

    private static final class HeroRequest {
        final ImageView image;
        final java.util.List<Object> models;
        final java.util.function.IntConsumer selected;
        final int width, height;
        final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
        com.bumptech.glide.request.target.CustomTarget<Drawable> target;
        Runnable cancelResolve;
        HeroRequest(ImageView image, java.util.List<Object> models, java.util.function.IntConsumer selected, int width, int height) {
            this.image=image; this.models=models; this.selected=selected;
            this.width=Math.min(width,960); this.height=Math.min(height,540);
        }
        boolean active() { return image.getTag(R.id.startup_home_artwork) == this; }
        void clear() { if(cancelResolve!=null)cancelResolve.run(); cancelResolve=null; image.animate().cancel(); image.setImageDrawable(null); if (target != null) Glide.with(image).clear(target); target=null; }
        void load(int index) {
            if (!active() || models.isEmpty()) return;
            clear();
            if (index >= Math.min(models.size(),8)) {
                image.setImageDrawable(null);
                selected.accept(0);
                diagnostic("no_panorama",0,0,0);
                return;
            }
            diagnostic("candidate",index,0,0);
            target = new com.bumptech.glide.request.target.CustomTarget<Drawable>(width,height) {
                @Override public void onResourceReady(Drawable art, com.bumptech.glide.request.transition.Transition<? super Drawable> transition) {
                    final int w=art.getIntrinsicWidth(), h=art.getIntrinsicHeight();
                    main.post(() -> {
                        if (!active()) return;
                        if (w < h * 1.35f) { load(index+1); return; }
                        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        image.animate().cancel();
                        image.setImageDrawable(art);
                        image.setAlpha(0.55f);
                        image.animate().alpha(1f).setDuration(700).start();
                        selected.accept(index);
                        diagnostic("panoramic",index,w,h);
                    });
                }
                @Override public void onLoadFailed(Drawable ignored) { main.post(() -> load(index+1)); }
                @Override public void onLoadCleared(Drawable ignored) { }
            };
            final com.bumptech.glide.request.target.CustomTarget<Drawable> currentTarget=target;
            cancelResolve=HomePublicArtwork.resolve(models.get(index), source -> {
                if (!active() || target!=currentTarget) return;
                Glide.with(image).load(source).dontTransform().skipMemoryCache(true)
                    .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565).disallowHardwareConfig()
                    .downsample(com.bumptech.glide.load.resource.bitmap.DownsampleStrategy.AT_MOST)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(currentTarget);
            });
        }
    }
    private static void diagnostic(String stage, int index, int width, int height) {
        Log.println(Log.INFO,"HomeArtwork","hero="+stage+" candidate="+index+" width="+width+" height="+height);
    }
}
