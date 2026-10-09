package com.drbep.tvplayer.netflix;

import android.util.Log;
import android.util.Base64;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONTokener;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

/** Manifest negotiation only. No segment download or content-license request. */
final class NetflixManifest {
    static void request(NetflixPlaybackActivity activity, NativeMslSession session,
            JSONObject masterToken, String identity, long viewableId) throws Exception {
        boolean uhd = activity.getIntent().getBooleanExtra("uhd", false);
        try {
            requestProfile(activity, session, masterToken, identity, viewableId, uhd);
        } catch (NetflixTracks.Unavailable unavailable) {
            if (!uhd || activity.isFinishing()) throw unavailable;
            // One lower-profile retry. Account authentication and DRM remain unchanged.
            Log.w("DRBEPNativeCDM", "netflix_uhd_unavailable_retry_hd=true " + unavailable.diagnostic);
            requestProfile(activity, session, masterToken, identity, viewableId, false);
        }
    }
    private static void requestProfile(NetflixPlaybackActivity activity, NativeMslSession session,
            JSONObject masterToken, String identity, long viewableId, boolean requestUhd) throws Exception {
        if (viewableId <= 0) throw new IllegalArgumentException("Missing title id");
        Map<String, String> cookies = SessionVault.load(activity, identity);
        byte[] body = null;
        HttpsURLConnection connection = null;
        try {
            JSONObject auth = new JSONObject().put("scheme", "NETFLIXID")
                    .put("authdata", new JSONObject().put("netflixid", cookies.get("NetflixId"))
                            .put("securenetflixid", cookies.get("SecureNetflixId")));
            // TV endpoint profile identifiers from the project reference. drmType below
            // remains widevine; the profile name alone does not select the CDM.
            JSONArray profiles = new JSONArray().put("playready-h264bpl30-dash").put("playready-h264mpl30-dash")
                    .put("playready-h264mpl31-dash").put("playready-h264mpl40-dash").put("playready-h264hpl30-dash")
                    .put("playready-h264hpl31-dash").put("playready-h264hpl40-dash").put("heaac-2-dash")
                    .put("webvtt-lssdh-ios8");
            JSONObject params = new JSONObject().put("viewableId", viewableId).put("profiles", profiles)
                    .put("type", "standard").put("flavor", "STANDARD").put("manifestVersion", "v3")
                    .put("playbackContextId", java.util.UUID.randomUUID().toString())
                    .put("usePsshBox", true).put("useHttpsStreams", true)
                    .put("drmType", "widevine").put("drmVersion", 0)
                    .put("isNonMember", false).put("isUIAutoPlay", false)
                    .put("isBranching", false).put("showAllSubDubTracks", true)
                    .put("imageSubtitleHeight", 720);
            if (requestUhd) {
                boolean capable = false;
                for (android.media.MediaCodecInfo codec : new android.media.MediaCodecList(
                        android.media.MediaCodecList.ALL_CODECS).getCodecInfos()) {
                    if (codec.isEncoder()) continue;
                    for (String mime : codec.getSupportedTypes()) {
                        if (!mime.equalsIgnoreCase("video/hevc")) continue;
                        android.media.MediaCodecInfo.CodecCapabilities caps = codec.getCapabilitiesForType(mime);
                        boolean main10 = false;
                        for (android.media.MediaCodecInfo.CodecProfileLevel profile : caps.profileLevels)
                            if (profile.profile == android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10)
                                main10 = true;
                        boolean secure = caps.isFeatureSupported(android.media.MediaCodecInfo.CodecCapabilities.FEATURE_SecurePlayback);
                        boolean uhd = caps.getVideoCapabilities().areSizeAndRateSupported(3840, 2160, 30);
                        Log.i("DRBEPNativeCDM", "hevc_decoder=" + codec.getName() + " main10=" + main10
                                + " secure=" + secure + " uhd30=" + uhd);
                        capable |= main10 && secure && uhd;
                    }
                }
                android.view.Display display = activity.getWindowManager().getDefaultDisplay();
                android.view.Display.Mode mode = display.getMode();
                Log.w("DRBEPNativeCDM", "display_active=" + mode.getPhysicalWidth() + "x" + mode.getPhysicalHeight());
                for (android.view.Display.Mode supported : display.getSupportedModes())
                    Log.i("DRBEPNativeCDM", "display_supported=" + supported.getPhysicalWidth() + "x" + supported.getPhysicalHeight());
                int connected = session.connectedHdcpLevel();
                int maximum = session.maxHdcpLevel();
                Log.w("DRBEPNativeCDM", "hdcp_connected=" + connected + " hdcp_max=" + maximum);
                if (!capable) throw new IllegalStateException("No secure Main10 UHD decoder");
                profiles = new JSONArray();
                for (String level : new String[] {"30", "31", "40", "41", "50", "51"})
                    profiles.put("hevc-main10-L" + level + "-dash-cenc");
                profiles.put("heaac-2-dash").put("webvtt-lssdh-ios8");
                params.put("profiles", profiles);
                JSONArray hdcp = new JSONArray();
                int[] levels = {android.media.MediaDrm.HDCP_V1, android.media.MediaDrm.HDCP_V2,
                        android.media.MediaDrm.HDCP_V2_1, android.media.MediaDrm.HDCP_V2_2,
                        android.media.MediaDrm.HDCP_V2_3};
                String[] names = {"1.0", "2.0", "2.1", "2.2", "2.3"};
                for (int i = 0; i < levels.length; i++)
                    if (maximum >= levels[i] && maximum <= android.media.MediaDrm.HDCP_V2_3) hdcp.put(names[i]);
                if (hdcp.length() > 0 && connected >= android.media.MediaDrm.HDCP_NONE
                        && connected <= android.media.MediaDrm.HDCP_V2_3)
                    params.put("videoOutputInfo", new JSONArray().put(new JSONObject()
                            .put("type", "DigitalVideoOutputDescriptor").put("outputType", "digitalOther")
                            .put("isHdcpEngaged", connected >= android.media.MediaDrm.HDCP_V1
                                    && connected <= android.media.MediaDrm.HDCP_V2_3)
                            .put("supportedHdcpVersions", hdcp)));
                Log.i("DRBEPNativeCDM", "manifest_requested_hevc_uhd=true");
            }
            JSONObject application = new JSONObject().put("version", 2).put("url", "/manifest")
                    .put("languages", new JSONArray().put("es-ES")).put("params", params)
                    .put("common", new JSONObject().put("application", "DRBEP-Offline-Native/1")
                            .put("platform", "android"));
            long messageId = new SecureRandom().nextInt(Integer.MAX_VALUE - 1);
            body = MslMessages.request(session, masterToken, identity, messageId,
                    application.toString().getBytes(StandardCharsets.UTF_8), auth);
            connection = (HttpsURLConnection) new URL(
                    "https://nrdp25.prod.ftl.netflix.com/playapi/nrdjs/manifest/1").openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(25000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Encoding", "msl_v1");
            connection.setRequestProperty("User-Agent", "DRBEP-Offline-Native/1");
            connection.setRequestProperty("X-Netflix.Client.Request.Name", "manifest");
            connection.setRequestProperty("X-Netflix.Request.NonJson.Headers", "true");
            connection.setRequestProperty("X-DeviceModel", android.os.Build.MODEL);
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = connection.getOutputStream()) { out.write(body); }
            int status = connection.getResponseCode();
            Log.i("DRBEPNativeCDM", "manifest_http=" + status);
            if (status != 200) throw new java.io.IOException("Manifest HTTP failure");
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = input.read(buffer)) != -1) {
                    if (response.size() + n > MslMessages.MAX_BYTES)
                        throw new java.io.IOException("Manifest response too large");
                    response.write(buffer, 0, n);
                }
            }
            String wire = new String(response.toByteArray(), StandardCharsets.UTF_8);
            JSONObject first = (JSONObject) new JSONTokener(wire).nextValue();
            if (first.has("errordata")) {
                JSONObject error = new JSONObject(new String(Base64.decode(first.getString("errordata"),
                        Base64.DEFAULT), StandardCharsets.UTF_8));
                Log.i("DRBEPNativeCDM", "manifest_msl_error=" + error.optInt("errorcode", -1)
                        + " internal=" + error.optInt("internalcode", -1));
                throw new java.io.IOException("Manifest MSL failure");
            }
            byte[] clear = MslMessages.response(session, wire, messageId + 1);
            try {
                Object result = new JSONTokener(new String(clear, StandardCharsets.UTF_8)).nextValue();
                Log.i("DRBEPNativeCDM", "manifest_response_authenticated=true");
                if (activity.isFinishing()) return;
                NetflixPlayer.play(activity, result, session, masterToken, identity, auth);
            } finally { java.util.Arrays.fill(clear, (byte) 0); }
        } finally {
            cookies.clear();
            if (body != null) java.util.Arrays.fill(body, (byte) 0);
            if (connection != null) connection.disconnect();
        }
    }

}
