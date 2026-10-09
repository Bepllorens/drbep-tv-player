package com.drbep.tvplayer.netflix;

import android.util.Base64;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.net.ssl.HttpsURLConnection;

/** Bounded interoperability probe, not a production Netflix transport. */
final class MslHandshake {
    private static final String TAG = "DRBEPNativeCDM";
    private static final String ENDPOINT =
            "https://nrdp25.prod.ftl.netflix.com/nq/nrdjs/pbo_tokens/%5E1.0.0/router";

    static void exchange(NativeMslSession session, byte[] challenge, String identity, boolean login, NetflixPlaybackActivity activity) throws Exception {
        long id = new SecureRandom().nextInt(Integer.MAX_VALUE - 1);
        JSONObject header = new JSONObject()
                .put("messageid", id).put("renewable", true).put("handshake", true)
                .put("timestamp", System.currentTimeMillis() / 1000)
                .put("recipient", "Netflix").put("nonreplayable", false)
                .put("capabilities", new JSONObject().put("compressionalgos", new JSONArray())
                        .put("languages", new JSONArray().put("en-US"))
                        .put("encoderformats", new JSONArray().put("JSON")))
                .put("keyrequestdata", new JSONArray().put(new JSONObject().put("scheme", "WIDEVINE")
                        .put("keydata", new JSONObject().put("keyrequest", b64(challenge)))));
        JSONObject message = new JSONObject()
                // Native Android reference separates NONE entity identity from
                // WIDEVINE key exchange. Identity belongs to this installation.
                .put("entityauthdata", new JSONObject().put("scheme", "NONE")
                        .put("authdata", new JSONObject().put("identity", identity)))
                .put("headerdata", b64(header.toString().getBytes(StandardCharsets.UTF_8)))
                .put("signature", "");
        JSONObject end = new JSONObject().put("messageid", id).put("data", "")
                .put("sequencenumber", 1).put("endofmsg", true);
        JSONObject payload = new JSONObject()
                .put("payload", b64(end.toString().getBytes(StandardCharsets.UTF_8)))
                .put("signature", "");
        byte[] body = (message.toString() + payload.toString()).getBytes(StandardCharsets.UTF_8);
        HttpsURLConnection connection = (HttpsURLConnection) new URL(ENDPOINT).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(25000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        // The nrdjs router requires this marker before it parses the MSL body.
        connection.setRequestProperty("Content-Encoding", "msl_v1");
        connection.setRequestProperty("User-Agent", "DRBEP-Offline-Native/1");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Language", "en-US");
        connection.setRequestProperty("X-Netflix.Client.Request.Name", "mintCookies");
        connection.setRequestProperty("X-Netflix.request.attempt", "1");
        connection.setRequestProperty("X-Netflix.Request.NonJson.Headers", "true");
        connection.setRequestProperty("X-DeviceModel", android.os.Build.MODEL);
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(body.length);
        try {
            try (OutputStream stream = connection.getOutputStream()) { stream.write(body); }
            int status = connection.getResponseCode();
            Log.i(TAG, "msl_http=" + status);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
                if (input == null) { Log.i(TAG, "msl_response_empty=true"); return; }
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (bytes.size() + count > 1024 * 1024)
                        throw new java.io.IOException("Oversized MSL response");
                    bytes.write(buffer, 0, count);
                }
            }
            String responseText = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            if (status != 200) {
                String lower = responseText.toLowerCase(java.util.Locale.ROOT);
                Log.i(TAG, "msl_error_mentions_esn=" + lower.contains("esn")
                        + " identity=" + lower.contains("identity")
                        + " header=" + lower.contains("header")
                        + " authentication=" + lower.contains("authenticat")
                        + " parse=" + lower.contains("pars"));
                if (!responseText.trim().startsWith("{")) {
                    Log.i(TAG, "msl_error_json=false");
                    return;
                }
            }
            JSONTokener responseTokens = new JSONTokener(responseText);
            Object first = responseTokens.nextValue();
            if (!(first instanceof JSONObject)) throw new IllegalStateException("Invalid MSL response");
            JSONObject response = (JSONObject) first;
            if (response.has("errordata")) {
                JSONObject error = decodedObject(response.getString("errordata"));
                // Never log remote free-text messages, identities or full responses.
                Log.i(TAG, "msl_error_code=" + error.optInt("errorcode", -1)
                        + " internal_code=" + error.optInt("internalcode", -1));
                String description = (error.optString("errormsg", "") + " "
                        + error.optString("usermsg", "")).toLowerCase(java.util.Locale.ROOT);
                StringBuilder topics = new StringBuilder();
                for (String word : new String[] {"entity", "identity", "esn", "scheme", "key",
                        "request", "unsupported", "invalid", "device", "widevine", "parse",
                        "authentication", "expired", "recipient", "sender", "provision", "signature",
                        "type", "recognized", "recognised", "not", "supported", "unknown",
                        "registered", "disabled", "permitted", "allowed"}) {
                    if (description.contains(word)) topics.append(word).append(',');
                }
                Log.i(TAG, "msl_error_topics=" + topics);
                return;
            }
            if (status != 200) {
                Object code = response.opt("code");
                String safeCode = String.valueOf(code);
                Log.i(TAG, "msl_error_json=true code="
                        + (safeCode.matches("[A-Z0-9_]{1,64}") ? safeCode : "unavailable"));
                return;
            }
            JSONObject returnedHeader = decodedObject(response.getString("headerdata"));
            if (returnedHeader.getLong("messageid") != id + 1)
                throw new IllegalStateException("Unexpected MSL response id");
            JSONObject keyResponse = returnedHeader.getJSONObject("keyresponsedata");
            if (!"WIDEVINE".equals(keyResponse.getString("scheme")))
                throw new IllegalStateException("Unexpected key exchange scheme");
            JSONObject data = keyResponse.getJSONObject("keydata");
            session.acceptKeyResponse(decode(data.getString("cdmkeyresponse")),
                    decode(data.getString("encryptionkeyid")), decode(data.getString("hmackeyid")));
            Log.i(TAG, "msl_key_response_accepted=true");
            byte[] sample = "DRBEP MSL local crypto check".getBytes(StandardCharsets.UTF_8);
            byte[] signature = session.sign(sample);
            Log.i(TAG, "msl_sign_verify=" + session.verify(sample, signature));
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            Log.i(TAG, "msl_encrypt_block=" + (session.encryptBlocks(new byte[16], iv).length == 16));
            byte[] envelope = session.encryptEnvelope(sample);
            byte[] envelopeSignature = session.sign(envelope);
            byte[] recovered = session.decryptVerifiedEnvelope(envelope, envelopeSignature);
            Log.i(TAG, "msl_authenticated_roundtrip=" + java.util.Arrays.equals(sample, recovered));
            envelope[envelope.length / 2] ^= 1;
            boolean rejected = false;
            try { session.decryptVerifiedEnvelope(envelope, envelopeSignature); }
            catch (SecurityException expected) { rejected = true; }
            Log.i(TAG, "msl_modified_message_rejected=" + rejected);
            java.util.Arrays.fill(recovered, (byte) 0);
            if (responseTokens.nextClean() != 0) {
                responseTokens.back();
                byte[] serverData = MslMessages.payloads(session, responseTokens, id + 1);
                Log.i(TAG, "msl_server_payload_authenticated=true empty=" + (serverData.length == 0));
                java.util.Arrays.fill(serverData, (byte) 0);
            } else {
                Log.i(TAG, "msl_handshake_payload_absent=true");
            }
            JSONObject masterToken = keyResponse.getJSONObject("mastertoken");
            MslMessages.selfTest(session, masterToken);
            long titleId = activity.getIntent().getLongExtra("manifest_title", 0);
            if (login) {
                transportProbe(session, masterToken, identity, true, activity);
                if (!activity.accountLinkConfirmed) {
                    if (!activity.isFinishing() && !activity.isDestroyed())
                        activity.showError("Vinculación sin completar. Si había una sesión anterior, se ha conservado. Puedes reintentar o volver a TV.");
                    return;
                }
            }
            if (titleId > 0 && !activity.isFinishing()) NetflixManifest.request(activity, session, masterToken, identity, titleId);
        } finally {
            java.util.Arrays.fill(body, (byte) 0);
            connection.disconnect();
        }
    }

    private static void transportProbe(NativeMslSession session, JSONObject masterToken, String identity, boolean login, NetflixPlaybackActivity activity)
            throws Exception {
        long messageId = new SecureRandom().nextInt(Integer.MAX_VALUE - 1);
        // Device-session request only, with this client's own name. No account
        // credentials, copied SDK identity or movie request are supplied.
        JSONObject applicationRequest = new JSONObject().put("version", 2)
                .put("url", "/mintCookies").put("languages", new JSONArray().put("en-US"))
                .put("params", new JSONObject()).put("common", new JSONObject()
                        .put("application", "DRBEP-Offline-Native/1").put("platform", "android"));
        byte[] request = MslMessages.request(session, masterToken, identity, messageId,
                applicationRequest.toString().getBytes(StandardCharsets.UTF_8));
        HttpsURLConnection connection = (HttpsURLConnection) new URL(ENDPOINT).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(25000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Encoding", "msl_v1");
        connection.setRequestProperty("User-Agent", "DRBEP-Offline-Native/1");
        connection.setRequestProperty("X-Netflix.Client.Request.Name", "mintCookies");
        connection.setRequestProperty("X-Netflix.Request.NonJson.Headers", "true");
        connection.setRequestProperty("X-Netflix.request.attempt", "1");
        connection.setRequestProperty("X-DeviceModel", android.os.Build.MODEL);
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(request.length);
        try {
            try (OutputStream output = connection.getOutputStream()) { output.write(request); }
            int status = connection.getResponseCode();
            Log.i(TAG, "msl_transport_http=" + status);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
                if (input == null) return;
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (bytes.size() + count > MslMessages.MAX_BYTES)
                        throw new java.io.IOException("MSL transport response too large");
                    bytes.write(buffer, 0, count);
                }
            }
            String text = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            if (!text.trim().startsWith("{")) {
                Log.i(TAG, "msl_transport_response_json=false empty=" + text.trim().isEmpty());
                return;
            }
            Object firstValue = new JSONTokener(text).nextValue();
            if (!(firstValue instanceof JSONObject)) {
                Log.i(TAG, "msl_transport_response_json=false");
                return;
            }
            JSONObject first = (JSONObject) firstValue;
            if (first.has("errordata")) {
                JSONObject error = decodedObject(first.getString("errordata"));
                Log.i(TAG, "msl_transport_error_code=" + error.optInt("errorcode", -1)
                        + " internal_code=" + error.optInt("internalcode", -1));
                return;
            }
            if (status != 200) {
                String code = String.valueOf(first.opt("code"));
                Log.i(TAG, "msl_transport_router_code="
                        + (code.matches("[A-Z0-9_]{1,64}") ? code : "unavailable"));
                return;
            }
            byte[] application = MslMessages.response(session, text, messageId + 1);
            Log.i(TAG, "msl_transport_response_authenticated=true empty=" + (application.length == 0));
            boolean netflixId = false, secureNetflixId = false, nfvdid = false;
            java.util.Map<String, String> deviceCookies = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, java.util.List<String>> entry : connection.getHeaderFields().entrySet()) {
                if (!"Set-Cookie".equalsIgnoreCase(entry.getKey())) continue;
                for (String line : entry.getValue()) {
                    for (java.net.HttpCookie cookie : java.net.HttpCookie.parse(line)) {
                        if (cookie.getValue().isEmpty() || cookie.hasExpired()) continue;
                        if ("NetflixId".equals(cookie.getName()) || "SecureNetflixId".equals(cookie.getName())
                                || "nfvdid".equals(cookie.getName()))
                            deviceCookies.put(cookie.getName(), cookie.getValue());
                        netflixId |= "NetflixId".equals(cookie.getName());
                        secureNetflixId |= "SecureNetflixId".equals(cookie.getName());
                        nfvdid |= "nfvdid".equals(cookie.getName());
                    }
                }
            }
            Log.i(TAG, "msl_mint_cookie_present netflix_id=" + netflixId
                    + " secure_netflix_id=" + secureNetflixId + " nfvdid=" + nfvdid);
            if (application.length > 0) {
                Object result = new JSONTokener(new String(application, StandardCharsets.UTF_8)).nextValue();
                if (result instanceof JSONObject) {
                    JSONObject object = (JSONObject) result;
                    Log.i(TAG, "msl_application_shape object=true error=" + object.has("error")
                            + " result=" + object.has("result") + " status_numeric="
                            + (object.opt("status") instanceof Number));
                } else {
                    Log.i(TAG, "msl_application_shape object=false array=" + (result instanceof JSONArray));
                }
            }
            java.util.Arrays.fill(application, (byte) 0);
            try { if (login) NativeLogin.initiate(identity, deviceCookies, activity); }
            finally { deviceCookies.clear(); }
        } finally {
            java.util.Arrays.fill(request, (byte) 0);
            connection.disconnect();
        }
    }

    private static String b64(byte[] bytes) { return Base64.encodeToString(bytes, Base64.NO_WRAP); }
    private static byte[] decode(String text) { return Base64.decode(text, Base64.DEFAULT); }
    private static JSONObject decodedObject(String text) throws Exception {
        return new JSONObject(new String(decode(text), StandardCharsets.UTF_8));
    }
}
