package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Metadata of the active encoded video track, never a bandwidth estimate. */
final class VodStreamInfo {
    static String label(int width, int height, String mime, String codecs, String hdr, int bitrate) {
        List<String> parts = new ArrayList<>();
        if (width > 0 && height > 0) {
            if (width >= 7680) parts.add("8K");
            else if (width >= 3840) parts.add("4K");
            else if (width >= 1920) parts.add("Full HD");
            else if (width >= 1280) parts.add("HD");
            parts.add(width + " × " + height);
        }
        if (hdr != null && !hdr.isEmpty()) parts.add(hdr);
        String type = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        String codec = codecs == null ? "" : codecs.toLowerCase(Locale.ROOT);
        if (type.equals("video/hevc") || codec.startsWith("hev1") || codec.startsWith("hvc1")
                || codec.startsWith("dvhe") || codec.startsWith("dvh1")) parts.add("HEVC");
        else if (type.equals("video/avc") || codec.startsWith("avc1") || codec.startsWith("avc3")) parts.add("H.264");
        else if (type.equals("video/av01") || codec.startsWith("av01")) parts.add("AV1");
        else if (type.equals("video/x-vnd.on2.vp9") || codec.startsWith("vp09")) parts.add("VP9");
        if (bitrate > 0) parts.add(String.format(Locale.forLanguageTag("es-ES"), "%.1f Mb/s", bitrate / 1_000_000d));
        return String.join(" · ", parts);
    }
}
