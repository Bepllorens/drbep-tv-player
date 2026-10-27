package com.drbep.tvplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;

/** Output changes are hints; the idle timeout covers TVs that do not report power state. */
final class PlaybackOutputMonitor {
    interface Listener { void changed(boolean available); }
    private final Context context;
    private final Handler handler;
    private final Listener listener;
    private boolean started, hdmiSeen, hdmiConnected = true, screenOn = true, dreaming;
    private AudioManager audio;
    private final AudioDeviceCallback audioCallback = new AudioDeviceCallback() {
        @Override public void onAudioDevicesAdded(AudioDeviceInfo[] devices) { for (AudioDeviceInfo d : devices) if (hdmi(d)) { hdmiSeen = true; hdmiConnected = true; emit(); } }
        @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] devices) { for (AudioDeviceInfo d : devices) if (hdmi(d)) { hdmiSeen = true; hdmiConnected = false; emit(); } }
    };
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) {
            String action = intent.getAction();
            if (AudioManager.ACTION_HDMI_AUDIO_PLUG.equals(action)) {
                hdmiSeen = true;
                hdmiConnected = intent.getIntExtra(AudioManager.EXTRA_AUDIO_PLUG_STATE, 1) != 0;
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) screenOn = false;
            else if (Intent.ACTION_SCREEN_ON.equals(action)) screenOn = true;
            else if (Intent.ACTION_DREAMING_STARTED.equals(action)) dreaming = true;
            else if (Intent.ACTION_DREAMING_STOPPED.equals(action)) dreaming = false;
            emit();
        }
    };
    PlaybackOutputMonitor(Context context, Handler handler, Listener listener) {
        this.context = context; this.handler = handler; this.listener = listener;
    }
    boolean available() { return screenOn && !dreaming && (!hdmiSeen || hdmiConnected); }
    private void emit() { if (started) listener.changed(available()); }
    private static boolean hdmi(AudioDeviceInfo d) {
        int type = d.getType();
        return type == AudioDeviceInfo.TYPE_HDMI || type == AudioDeviceInfo.TYPE_HDMI_ARC ||
                (Build.VERSION.SDK_INT >= 31 && type == AudioDeviceInfo.TYPE_HDMI_EARC);
    }
    void start() {
        if (started) return;
        started = true;
        audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= 23 && audio != null) {
            audio.registerAudioDeviceCallback(audioCallback, handler);
            for (AudioDeviceInfo d : audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) if (hdmi(d)) {hdmiSeen = true; hdmiConnected = true;}
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.ACTION_HDMI_AUDIO_PLUG);
        filter.addAction(Intent.ACTION_SCREEN_OFF); filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_DREAMING_STARTED); filter.addAction(Intent.ACTION_DREAMING_STOPPED);
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else context.registerReceiver(receiver, filter);
        emit();
    }
    void close() {
        if (!started) return;
        started = false;
        try { context.unregisterReceiver(receiver); } catch (IllegalArgumentException ignored) {}
        if (Build.VERSION.SDK_INT >= 23 && audio != null) audio.unregisterAudioDeviceCallback(audioCallback);
    }
}
