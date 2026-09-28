package com.drbep.tvplayer.netflix;

import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Bounded, authenticated MSL framing. No credentials or tokens are logged. */
final class MslMessages {
    static final int MAX_BYTES = 1024 * 1024;

    static byte[] request(NativeMslSession session, JSONObject masterToken, String identity, long id,
            byte[] applicationData) throws Exception {
        return request(session, masterToken, identity, id, applicationData, null);
    }

    static byte[] request(NativeMslSession session, JSONObject masterToken, String identity, long id,
            byte[] applicationData, JSONObject userAuth) throws Exception {
        JSONObject header = new JSONObject().put("messageid", id)
                .put("renewable", false).put("handshake", false)
                .put("timestamp", System.currentTimeMillis() / 1000)
                .put("recipient", "Netflix").put("sender", identity).put("nonreplayable", false)
                .put("capabilities", new JSONObject().put("compressionalgos", new JSONArray())
                        .put("languages", new JSONArray().put("en-US"))
                        .put("encoderformats", new JSONArray().put("JSON")));
        if (userAuth != null) header.put("userauthdata", userAuth);
        JSONObject framedHeader = envelope(session, "headerdata", header)
                .put("mastertoken", masterToken);
        JSONObject chunk = new JSONObject().put("messageid", id)
                .put("sequencenumber", 1).put("endofmsg", true)
                .put("data", b64(applicationData));
        byte[] result = (framedHeader.toString() + envelope(session, "payload", chunk))
                .getBytes(StandardCharsets.UTF_8);
        if (result.length > MAX_BYTES) throw new IllegalArgumentException("Request too large");
        return result;
    }

    static JSONObject envelope(NativeMslSession session, String field, JSONObject data)
            throws Exception {
        byte[] bytes = session.encryptEnvelope(data.toString().getBytes(StandardCharsets.UTF_8));
        return new JSONObject().put(field, b64(bytes)).put("signature", b64(session.sign(bytes)));
    }

    static byte[] response(NativeMslSession session, String text, long expectedId) throws Exception {
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            throw new IllegalArgumentException("Response too large");
        JSONTokener tokens = new JSONTokener(text);
        JSONObject outer = nextObject(tokens);
        if (outer.has("errordata")) {
            // An unverified error cannot establish an authenticated channel.
            throw new SecurityException("Unauthenticated MSL error");
        }
        JSONObject header = decrypt(session, outer, "headerdata");
        if (header.getLong("messageid") != expectedId || header.optBoolean("handshake")
                || header.has("keyresponsedata"))
            throw new SecurityException("Unexpected MSL header");
        return payloads(session, tokens, expectedId);
    }

    static byte[] payloads(NativeMslSession session, JSONTokener tokens, long expectedId)
            throws Exception {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        boolean end = false;
        int sequence = 1;
        while (tokens.nextClean() != 0) {
            tokens.back();
            if (end || sequence > 128) throw new SecurityException("Unexpected MSL chunk");
            JSONObject chunk = decrypt(session, nextObject(tokens), "payload");
            if (chunk.getLong("messageid") != expectedId
                    || chunk.getLong("sequencenumber") != sequence++)
                throw new SecurityException("MSL chunk binding mismatch");
            if (chunk.has("compressionalgo"))
                throw new SecurityException("Compression not negotiated");
            byte[] data = Base64.decode(chunk.getString("data"), Base64.DEFAULT);
            if (result.size() + data.length > MAX_BYTES)
                throw new SecurityException("Application response too large");
            result.write(data);
            java.util.Arrays.fill(data, (byte) 0);
            end = chunk.optBoolean("endofmsg", false);
        }
        if (!end) throw new SecurityException("Incomplete MSL message");
        return result.toByteArray();
    }

    private static JSONObject decrypt(NativeMslSession session, JSONObject outer, String field)
            throws Exception {
        byte[] clear = session.decryptVerifiedEnvelope(
                Base64.decode(outer.getString(field), Base64.DEFAULT),
                Base64.decode(outer.getString("signature"), Base64.DEFAULT));
        try { return new JSONObject(new String(clear, StandardCharsets.UTF_8)); }
        finally { java.util.Arrays.fill(clear, (byte) 0); }
    }

    private static JSONObject nextObject(JSONTokener tokens) throws Exception {
        Object value = tokens.nextValue();
        if (!(value instanceof JSONObject)) throw new SecurityException("Invalid MSL object");
        return (JSONObject) value;
    }

    static void selfTest(NativeMslSession session, JSONObject token) throws Exception {
        byte[] sample = "DRBEP framing check".getBytes(StandardCharsets.UTF_8);
        String wire = new String(request(session, token, "DRBEP-local-test", 41, sample), StandardCharsets.UTF_8);
        byte[] recovered = response(session, wire, 41);
        if (!java.util.Arrays.equals(sample, recovered))
            throw new AssertionError("MSL framing roundtrip failed");
        java.util.Arrays.fill(recovered, (byte) 0);
        JSONTokener parts = new JSONTokener(wire);
        JSONObject header = nextObject(parts);
        JSONObject chunk = nextObject(parts);
        expectRejected(session, wire, 42); // Replay / wrong response binding.
        expectRejected(session, header.toString(), 41); // Truncated response.
        expectRejected(session, wire + chunk, 41); // Trailing chunk after end.
        JSONObject reordered = new JSONObject().put("messageid", 41)
                .put("sequencenumber", 2).put("endofmsg", true).put("data", b64(sample));
        expectRejected(session, header.toString() + envelope(session, "payload", reordered), 41);
        byte[] signature = Base64.decode(chunk.getString("signature"), Base64.DEFAULT);
        signature[0] ^= 1;
        chunk.put("signature", b64(signature));
        expectRejected(session, header.toString() + chunk, 41);
        android.util.Log.i("DRBEPNativeCDM", "msl_framing_checks_passed=6");
    }

    private static void expectRejected(NativeMslSession session, String text, long id)
            throws Exception {
        try { response(session, text, id); }
        catch (SecurityException expected) { return; }
        throw new AssertionError("Malformed MSL message accepted");
    }

    private static String b64(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }
}
