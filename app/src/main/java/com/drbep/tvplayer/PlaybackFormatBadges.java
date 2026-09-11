package com.drbep.tvplayer;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import java.util.Locale;

/** Describes selected encoded tracks, not the TV/receiver's negotiated output. */
final class PlaybackFormatBadges {
    static String label(Format video, Format audio) {
        String v = video(video);
        String a = audio(audio);
        return v.isEmpty() ? a : a.isEmpty() ? v : v + "  ·  " + a;
    }

    static String video(Format f) {
        if (f == null) return "";
        String mime = lower(f.sampleMimeType), codec = lower(f.codecs);
        if (mime.equals("video/dolby-vision") || codec.startsWith("dvhe")
                || codec.startsWith("dvh1") || codec.startsWith("dvav") || codec.startsWith("dva1"))
            return "DOLBY VISION";
        if (f.colorInfo == null) return "";
        if (f.colorInfo.colorTransfer == C.COLOR_TRANSFER_HLG) return "HDR · HLG";
        if (f.colorInfo.colorTransfer == C.COLOR_TRANSFER_ST2084) return "HDR · PQ";
        return "";
    }

    static String audio(Format f) {
        if (f == null) return "";
        String mime = lower(f.sampleMimeType);
        // Ordinary E-AC-3, multichannel audio or TrueHD alone do not prove Atmos.
        if (mime.equals("audio/eac3-joc")) return "DOLBY ATMOS";
        if (mime.equals("audio/eac3")) return "DOLBY DIGITAL PLUS";
        if (mime.equals("audio/ac3")) return "DOLBY DIGITAL";
        if (mime.equals("audio/true-hd")) return "DOLBY TRUEHD";
        return "";
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
