package com.drbep.tvplayer;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

final class VlcDirectPlayController {
    interface Host {
        void onBuffering();

        void onPlaying();

        void onPaused();

        void onFirstFrame();

        void onEnded();

        void onError(String reason);
    }

    private final Context context;
    private final VLCVideoLayout videoLayout;
    private final Host host;
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private boolean active;
    private boolean firstFrameReported;
    private long pendingStartMs;

    VlcDirectPlayController(Context context, VLCVideoLayout videoLayout, Host host) {
        this.context = context.getApplicationContext();
        this.videoLayout = videoLayout;
        this.host = host;
    }

    boolean isAvailable() {
        return videoLayout != null;
    }

    boolean isActive() {
        return active && mediaPlayer != null;
    }

    void play(String url, long startMs, boolean autoPlay) {
        if (!isAvailable() || url == null || url.trim().isEmpty()) {
            host.onError("URL de reproducción vacía");
            return;
        }
        stop();
        try {
            ensureLibVLC();
        } catch (RuntimeException | LinkageError error) {
            host.onError("No se pudo iniciar libVLC: " + error.getClass().getSimpleName());
            return;
        }
        firstFrameReported = false;
        pendingStartMs = Math.max(0L, startMs);
        mediaPlayer = new MediaPlayer(libVLC);
        mediaPlayer.setEventListener(this::onEvent);
        videoLayout.setBackgroundColor(Color.BLACK);
        videoLayout.setVisibility(View.VISIBLE);
        mediaPlayer.attachViews(videoLayout, null, false, false);
        Media media = new Media(libVLC, Uri.parse(url.trim()));
        media.addOption(":network-caching=3000");
        media.addOption(":http-reconnect");
        if (pendingStartMs > 0L) {
            media.addOption(String.format(Locale.US, ":start-time=%.3f", pendingStartMs / 1000d));
            pendingStartMs = 0L;
        }
        mediaPlayer.setMedia(media);
        media.release();
        active = true;
        if (autoPlay) {
            mediaPlayer.play();
        }
    }

    private void ensureLibVLC() {
        if (libVLC == null) {
            // LibVLC añade internamente opciones de plataforma; necesita una lista mutable.
            libVLC = new LibVLC(context, new ArrayList<>(Arrays.asList(
                    "--network-caching=3000",
                    "--http-reconnect"
            )));
        }
    }

    private void onEvent(MediaPlayer.Event event) {
        if (event == null || !active) {
            return;
        }
        videoLayout.post(() -> {
            if (!active) {
                return;
            }
            switch (event.type) {
                case MediaPlayer.Event.Opening:
                case MediaPlayer.Event.Buffering:
                    host.onBuffering();
                    break;
                case MediaPlayer.Event.Playing:
                    host.onPlaying();
                    break;
                case MediaPlayer.Event.Paused:
                    host.onPaused();
                    break;
                case MediaPlayer.Event.Vout:
                    reportFirstFrame();
                    break;
                case MediaPlayer.Event.EndReached:
                    host.onEnded();
                    break;
                case MediaPlayer.Event.EncounteredError:
                    host.onError("libVLC no pudo reproducir el contenido");
                    break;
                default:
                    break;
            }
        });
    }

    private void reportFirstFrame() {
        if (firstFrameReported) {
            return;
        }
        firstFrameReported = true;
        host.onFirstFrame();
    }

    boolean isPlaying() {
        return isActive() && mediaPlayer.isPlaying();
    }

    long getTime() {
        return isActive() ? Math.max(0L, mediaPlayer.getTime()) : 0L;
    }

    long getLength() {
        return isActive() ? Math.max(0L, mediaPlayer.getLength()) : 0L;
    }

    void setTime(long valueMs) {
        if (isActive()) {
            mediaPlayer.setTime(Math.max(0L, valueMs));
        }
    }

    void setPlayWhenReady(boolean playWhenReady) {
        if (!isActive()) {
            return;
        }
        if (playWhenReady) {
            mediaPlayer.play();
        } else {
            mediaPlayer.pause();
        }
    }

    void togglePlayback() {
        setPlayWhenReady(!isPlaying());
    }

    void setMuted(boolean muted) {
        if (isActive()) {
            mediaPlayer.setVolume(muted ? 0 : 100);
        }
    }

    void stop() {
        active = false;
        firstFrameReported = false;
        pendingStartMs = 0L;
        if (mediaPlayer != null) {
            mediaPlayer.setEventListener(null);
            mediaPlayer.stop();
            mediaPlayer.detachViews();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (videoLayout != null) {
            videoLayout.setVisibility(View.GONE);
        }
    }

    void release() {
        stop();
        if (libVLC != null) {
            libVLC.release();
            libVLC = null;
        }
    }
}
