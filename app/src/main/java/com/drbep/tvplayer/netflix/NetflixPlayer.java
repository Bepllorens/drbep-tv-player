package com.drbep.tvplayer.netflix;

import android.util.Log;
import android.view.SurfaceView;
import androidx.media3.common.*;
import androidx.media3.exoplayer.*;
import androidx.media3.exoplayer.drm.*;
import androidx.media3.exoplayer.source.*;
import androidx.media3.datasource.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Native playback with account-authorized track choices and DRBEP controls. */
final class NetflixPlayer {
    private static String identityKey(NetflixPlaybackActivity activity) { return "title:" + activity.getIntent().getLongExtra("manifest_title", 0); }
    public static void play(NetflixPlaybackActivity activity, Object manifest, NativeMslSession transport,
            JSONObject master, String identity, JSONObject auth) throws Exception {
        final NetflixTracks tracks = NetflixTracks.parse(manifest);
        if (tracks.subtitles.isEmpty()) Log.w("DRBEPNativeCDM", "subtitle_schema=" + NetflixTracks.subtitleSchema(manifest));
        final String licensePath = NetflixLicense.licensePath(manifest, 0);
        CountDownLatch done = new CountDownLatch(1), released = new CountDownLatch(1);
        ExoPlayer[] player = new ExoPlayer[1];
        NetflixFrameRate frameRate = new NetflixFrameRate();
        MediaDrmCallback callback = new MediaDrmCallback() {
            public MediaDrmCallback.Response executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) {
                throw new IllegalStateException("Device must already be provisioned");
            }
            public MediaDrmCallback.Response executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) {
                try {
                    byte[] requestId = new byte[16]; new java.security.SecureRandom().nextBytes(requestId);
                    Log.i("DRBEPNativeCDM", "media3_native_license_requested=true");
                    return new MediaDrmCallback.Response(NetflixLicense.exchange(transport, master, identity, auth,
                            requestId, request.getData(), licensePath));
                } catch (Exception e) { throw new IllegalStateException("Native license request failed"); }
            }
        };
        activity.runOnUiThread(() -> {
            try {
                if (activity.isFinishing() || activity.isDestroyed()) { done.countDown(); return; }
                androidx.media3.ui.PlayerView view = new androidx.media3.ui.PlayerView(activity);
                if (view.getVideoSurfaceView() instanceof SurfaceView) ((SurfaceView)view.getVideoSurfaceView()).setSecure(true);
                view.setKeepScreenOn(true); view.setUseController(false);
                DefaultDrmSessionManager manager = new DefaultDrmSessionManager.Builder()
                        .setUuidAndExoMediaDrmProvider(C.WIDEVINE_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                        .setMultiSession(true)
                        .build(callback);
                DefaultDataSource.Factory data = new DefaultDataSource.Factory(activity,
                        new DefaultHttpDataSource.Factory().setConnectTimeoutMs(15000).setReadTimeoutMs(15000));
                DefaultMediaSourceFactory sources = new DefaultMediaSourceFactory(data)
                        .setDrmSessionManagerProvider(item -> manager);
                DefaultRenderersFactory renderers = new DefaultRenderersFactory(activity);
                ExoPlayer p = new ExoPlayer.Builder(activity, renderers).build(); player[0] = p;
                p.addAnalyticsListener(new androidx.media3.exoplayer.analytics.AnalyticsListener() {
                    public void onAudioDecoderInitialized(EventTime time, String name, long timestamp, long duration) {
                        Log.i("DRBEPNativeCDM", "media3_audio_decoder=" + name);
                    }
                });
                p.setAudioAttributes(new androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true);
                view.setPlayer(p);
                p.setVideoFrameMetadataListener((presentationUs, releaseNs, format, mediaFormat) -> frameRate.record(presentationUs));
                activity.stopPlayback = () -> { p.pause(); done.countDown(); };
                p.addListener(new Player.Listener() {
                    public void onRenderedFirstFrame() { Log.i("DRBEPNativeCDM", "media3_first_frame=true"); }
                    public void onVideoSizeChanged(VideoSize size) { Log.i("DRBEPNativeCDM", "media3_video=" + size.width + "x" + size.height); }
                    public void onPlayerError(PlaybackException e) {
                        Log.i("DRBEPNativeCDM", "media3_error=" + e.errorCode);
                        Throwable cause = e.getCause();
                        for (int n=0; cause != null && n<5; n++, cause=cause.getCause())
                            Log.i("DRBEPNativeCDM", "media3_error_type=" + cause.getClass().getSimpleName());
                        activity.showError("No se pudo reproducir Netflix. Código: " + e.errorCode);
                        done.countDown();
                    }
                });
                long resume = activity.getSharedPreferences("netflix_resume", 0).getLong(identityKey(activity), 0);
                p.setMediaSource(source(sources, tracks, 0, tracks.defaultAudio(), -1), resume);
                p.prepare(); p.play();
                view.postDelayed(new Runnable() {
                    public void run() {
                        if(activity.isFinishing() || activity.isDestroyed() || done.getCount()==0)return;
                        if(p.getPlaybackState()==Player.STATE_READY && p.getCurrentPosition()>30000)
                            NetflixContinueStore.save(activity,activity.getIntent(),p.getCurrentPosition(),p.getDuration());
                        view.postDelayed(this,10000);
                    }
                },10000);
                activity.controls = new NetflixPlaybackControls(activity, p, view, tracks, frameRate, (video, audio, subtitle) -> {
                    frameRate.reset();
                    long position = p.getCurrentPosition();
                    boolean playing = p.getPlayWhenReady();
                    p.setMediaSource(source(sources, tracks, video, audio, subtitle), position);
                    p.prepare(); p.setPlayWhenReady(playing);
                });
                p.addListener(new Player.Listener() {
                    public void onPlaybackStateChanged(int state) { if (state == Player.STATE_ENDED) { activity.completed = true; done.countDown(); } }
                });
            } catch (Exception e) {
                Log.i("DRBEPNativeCDM", "media3_setup_error=" + e.getClass().getSimpleName()); activity.showError("No se pudo preparar el reproductor."); done.countDown();
            }
        });
        try { while (!done.await(1, TimeUnit.SECONDS)) { if (activity.isFinishing() || activity.isDestroyed()) break; } }
        finally {
            activity.runOnUiThread(() -> {
                try {
                    if (player[0] != null) {
                        androidx.media3.exoplayer.DecoderCounters count = player[0].getVideoDecoderCounters();
                        if (count != null) { count.ensureUpdated(); Log.i("DRBEPNativeCDM", "media3_rendered=" + count.renderedOutputBufferCount + " skipped=" + count.skippedOutputBufferCount); }
                        androidx.media3.exoplayer.DecoderCounters ac = player[0].getAudioDecoderCounters();
                        if (ac != null) { ac.ensureUpdated(); Log.i("DRBEPNativeCDM", "media3_audio_rendered=" + ac.renderedOutputBufferCount + " input=" + ac.queuedInputBufferCount); }
                        Log.i("DRBEPNativeCDM", "media3_state=" + player[0].getPlaybackState()
                            + " playing=" + player[0].isPlaying() + " suppression=" + player[0].getPlaybackSuppressionReason()
                            + " buffered_ms=" + player[0].getTotalBufferedDuration());
                        Log.i("DRBEPNativeCDM", "media3_position_ms=" + player[0].getCurrentPosition());
                        long position = activity.completed ? 0 : player[0].getCurrentPosition();
                        NetflixContinueStore.save(activity, activity.getIntent(), position, player[0].getDuration());
                        if (activity.controls != null) activity.controls.dispose();
                        activity.controls = null;
                        player[0].release();
                        activity.stopPlayback = null;
                    }
                } finally { released.countDown(); }
            });
            if (!released.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Player release timeout");
        }
    }
    private static MediaSource source(DefaultMediaSourceFactory factory, NetflixTracks tracks,
            int video, int audio, int subtitle) {
        MediaItem.Builder builder = new MediaItem.Builder().setUri(tracks.video.get(video).url)
                .setMimeType(MimeTypes.VIDEO_MP4)
                .setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID).build());
        if (subtitle >= 0) {
            NetflixTracks.Choice text = tracks.subtitles.get(subtitle);
            builder.setSubtitleConfigurations(java.util.Collections.singletonList(
                    new MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(text.url))
                        .setMimeType(MimeTypes.TEXT_VTT).setLanguage(text.language).setLabel(text.label)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()));
        }
        MediaSource videoSource = factory.createMediaSource(builder.build());
        if (audio < 0) return videoSource;
        MediaItem sound = new MediaItem.Builder().setUri(tracks.audio.get(audio).url)
                .setMimeType(MimeTypes.AUDIO_MP4)
                .setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID).build()).build();
        return new MergingMediaSource(videoSource, factory.createMediaSource(sound));
    }

}
