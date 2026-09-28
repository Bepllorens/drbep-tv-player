package com.drbep.tvplayer;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaDrm;
import android.util.Log;
import java.util.UUID;

final class HboNativeCapabilities {
    private static String cachedProfile;
    private static boolean hdr10Display;
    private static boolean tvDevice;
    static void configureDisplay(android.content.Context context) {
        tvDevice = (context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_TYPE_MASK)
                == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION;
        hdr10Display = false;
        if (android.os.Build.VERSION.SDK_INT < 24) return;
        android.view.WindowManager wm = (android.view.WindowManager) context.getSystemService(android.content.Context.WINDOW_SERVICE);
        if (wm == null) return;
        for (int type : wm.getDefaultDisplay().getHdrCapabilities().getSupportedHdrTypes()) {
            if (type == android.view.Display.HdrCapabilities.HDR_TYPE_HDR10) hdr10Display = true;
        }
    }
    static String drmScheme() {
        if (tvDevice && "AFTKRT".equals(android.os.Build.MODEL) && "uhd".equals(profile())) {
            try {
                if (MediaDrm.isCryptoSchemeSupported(UUID.fromString("9a04f079-9840-4286-ab92-e65be0885f95"))) return "playready";
            } catch (RuntimeException ignored) { }
        }
        return "widevine";
    }
    static String query() {
        String profile = profile();
        String query = "uhd".equals(profile) ? "?profile=uhd" : "";
        if (!tvDevice) return query;
        try {
            org.json.JSONObject device = new org.json.JSONObject();
            device.put("make", android.os.Build.MANUFACTURER);
            device.put("model", android.os.Build.MODEL);
            device.put("os", android.os.Build.VERSION.RELEASE);
            device.put("app", BuildConfig.VERSION_NAME);
            device.put("hdr10", hdr10Display && "uhd".equals(profile));
            String encoded = android.util.Base64.encodeToString(device.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING);
            return (query.isEmpty() ? "?profile=hd" : query) + "&client=" + encoded + "&drm=" + drmScheme();
        } catch (Exception ignored) { return query; }
    }
    static boolean supportsUhd(String securityLevel, boolean secureHevcMain10Uhd) {
        return "L1".equalsIgnoreCase(securityLevel) && secureHevcMain10Uhd;
    }
    static synchronized String profile() {
        if (cachedProfile != null) return cachedProfile;
        String level = "";
        boolean secureUhd = false;
        MediaDrm drm = null;
        try {
            drm = new MediaDrm(UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"));
            level = drm.getPropertyString("securityLevel");
            if ("L1".equalsIgnoreCase(level)) {
                for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos()) {
                    if (info.isEncoder()) continue;
                    for (String type : info.getSupportedTypes()) {
                        if (!"video/hevc".equalsIgnoreCase(type)) continue;
                        try {
                            MediaCodecInfo.CodecCapabilities cap = info.getCapabilitiesForType(type);
                            boolean main10 = false;
                            for (MediaCodecInfo.CodecProfileLevel profile : cap.profileLevels) {
                                if (profile.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10
                                        || profile.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10
                                        || profile.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus) main10 = true;
                            }
                            secureUhd |= main10 && cap.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_SecurePlayback)
                                    && cap.getVideoCapabilities().areSizeAndRateSupported(3840, 2160, 30);
                        } catch (RuntimeException ignored) { }
                    }
                }
            }
        } catch (Exception ignored) {
            // Unsupported/unavailable native DRM stays on the conservative profile.
        } finally {
            if (drm != null) {
                try { drm.release(); } catch (RuntimeException ignored) { }
            }
        }
        cachedProfile = supportsUhd(level, secureUhd) ? "uhd" : "hd";
        try { Log.w("HboNativeCapabilities", "HBO native profile=" + cachedProfile + " widevine=" + level + " secureHevcMain10Uhd=" + secureUhd); }
        catch (RuntimeException ignored) { }
        return cachedProfile;
    }
}
