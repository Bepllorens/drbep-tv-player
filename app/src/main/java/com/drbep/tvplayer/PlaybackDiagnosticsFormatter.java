package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class PlaybackDiagnosticsFormatter {
    private static final String SEPARATOR = "  ·  ";

    private PlaybackDiagnosticsFormatter() {
    }

    static String detailed(PlayerController.PlaybackDiagnostics diagnostics, String unknownLabel, Locale locale) {
        if (diagnostics == null || !diagnostics.hasVideoQuality()) {
            return clean(unknownLabel);
        }
        Locale safeLocale = locale == null ? Locale.getDefault() : locale;
        List<String> parts = new ArrayList<>();
        if (diagnostics.videoWidth > 0 && diagnostics.videoHeight > 0) {
            parts.add(diagnostics.videoWidth + "x" + diagnostics.videoHeight);
        }
        add(parts, diagnostics.videoCodec);
        if (diagnostics.videoFrameRate > 0f) {
            parts.add(String.format(safeLocale, "%.0f fps", diagnostics.videoFrameRate));
        }
        if (diagnostics.videoBitrate > 0) {
            parts.add(String.format(safeLocale, "%.1f Mbps", diagnostics.videoBitrate / 1_000_000f));
        }
        String audioCodec = clean(diagnostics.audioCodec);
        if (!audioCodec.isEmpty()) {
            parts.add("Audio " + audioCodec);
        }
        return parts.isEmpty() ? clean(unknownLabel) : join(parts);
    }

    static String compact(PlayerController.PlaybackDiagnostics diagnostics, Locale locale) {
        if (diagnostics == null || !diagnostics.hasVideoQuality()) {
            return "";
        }
        Locale safeLocale = locale == null ? Locale.getDefault() : locale;
        List<String> parts = new ArrayList<>();
        if (diagnostics.videoHeight > 0) {
            parts.add(diagnostics.videoWidth >= 3840 || diagnostics.videoHeight >= 2160
                    ? "4K"
                    : diagnostics.videoHeight + "p");
        } else if (diagnostics.videoWidth > 0) {
            parts.add(diagnostics.videoWidth + "px");
        }
        add(parts, compactCodec(diagnostics.videoCodec));
        if (diagnostics.videoFrameRate > 0f) {
            parts.add(String.format(safeLocale, "%.0f fps", diagnostics.videoFrameRate));
        }
        if (diagnostics.videoBitrate > 0) {
            parts.add(String.format(safeLocale, "%.1f Mbps", diagnostics.videoBitrate / 1_000_000f));
        }
        return join(parts);
    }

    static String compactCodec(String codec) {
        String value = clean(codec);
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("avc") || lower.contains("h264")) {
            return "H.264";
        }
        if (lower.contains("hevc") || lower.contains("h265") || lower.contains("hvc1") || lower.contains("hev1")) {
            return "H.265";
        }
        return value;
    }

    private static void add(List<String> parts, String value) {
        String clean = clean(value);
        if (!clean.isEmpty()) {
            parts.add(clean);
        }
    }

    private static String join(List<String> parts) {
        return String.join(SEPARATOR, parts);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
