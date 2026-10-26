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

    /** Compact facts about encoded tracks. Channel count never implies a speaker layout. */
    static String details(Format video, Format audio) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (video != null) {
            if (video.width > 0 && video.height > 0) parts.add(video.width + " × " + video.height);
            String hdr = video(video);
            if (!hdr.isEmpty()) parts.add(hdr);
            String mime = lower(video.sampleMimeType), codecs = lower(video.codecs);
            if (mime.equals("video/hevc") || codecs.startsWith("hvc1") || codecs.startsWith("hev1")
                    || codecs.startsWith("dvhe") || codecs.startsWith("dvh1")) parts.add("HEVC");
            else if (mime.equals("video/avc") || codecs.startsWith("avc1") || codecs.startsWith("avc3")) parts.add("H.264");
            else if (mime.equals("video/av01") || codecs.startsWith("av01")) parts.add("AV1");
            else if (mime.equals("video/x-vnd.on2.vp9") || codecs.startsWith("vp09")) parts.add("VP9");
        }
        if (video != null && video.bitrate > 0)
            parts.add(String.format(Locale.forLanguageTag("es-ES"), "%.1f Mb/s", video.bitrate / 1_000_000d));
        if (video != null && Float.isFinite(video.frameRate) && video.frameRate > 0)
            parts.add(String.format(Locale.forLanguageTag("es-ES"), "%.2f fps", video.frameRate));
        if (audio != null) {
            String label = audio(audio), mime = lower(audio.sampleMimeType);
            if (!label.isEmpty()) parts.add(label);
            else if (mime.equals("audio/mp4a-latm")) parts.add("AAC");
            else if (mime.equals("audio/opus")) parts.add("OPUS");
            else if (mime.equals("audio/flac")) parts.add("FLAC");
            else if (mime.equals("audio/mpeg")) parts.add("MP3");
            else if (mime.equals("audio/raw")) parts.add("PCM");
            else if (mime.equals("audio/vnd.dts")) parts.add("DTS");
            else if (mime.equals("audio/vnd.dts.hd")) parts.add("DTS-HD");
            if (audio.channelCount > 0 && audio.channelCount <= 32)
                parts.add(audio.channelCount + (audio.channelCount == 1 ? " canal" : " canales"));
        }
        String container = lower(video != null ? video.containerMimeType : audio == null ? "" : audio.containerMimeType);
        if (container.equals("video/mp4") || container.equals("audio/mp4")) parts.add("MP4");
        else if (container.equals("video/mp2t")) parts.add("TS");
        else if (container.equals("video/webm") || container.equals("audio/webm")) parts.add("WEBM");
        else if (container.equals("video/x-matroska") || container.equals("audio/x-matroska")) parts.add("MKV");
        return String.join(" · ", parts);
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
