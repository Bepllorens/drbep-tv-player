package com.drbep.tvplayer.netflix;

import android.media.MediaDrm;
import android.media.NotProvisionedException;
import android.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Prototype CDM adapter. It owns a dedicated DRM session, not the MediaDrm.
 * Transport, account authentication and Netflix client identity are external.
 * Key identifiers below are references; no method returns key material.
 */
final class NativeMslSession implements AutoCloseable {
    private final MediaDrm drm;
    private final byte[] sessionId;
    private final MediaDrm.CryptoSession crypto;
    interface Provisioner { void provision() throws Exception; }
    private final Provisioner provisioner;
    private byte[] encryptionKeyId;
    private byte[] hmacKeyId;
    private byte[] offlineKeySetId;
    private boolean requested;
    private boolean closed;

    int connectedHdcpLevel() { return drm.getConnectedHdcpLevel(); }
    int maxHdcpLevel() { return drm.getMaxHdcpLevel(); }

    NativeMslSession(MediaDrm drm, Provisioner provisioner) throws Exception {
        this.drm = drm;
        this.provisioner = provisioner;
        sessionId = drm.openSession();
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            try {
                android.util.Log.w("DRBEPNativeCDM", "native_session_security="
                        + drm.getSecurityLevel(sessionId));
            } catch (RuntimeException unavailable) {
                android.util.Log.w("DRBEPNativeCDM", "native_session_security_unavailable=true");
            }
        }
        try {
            crypto = drm.getCryptoSession(sessionId, "AES/CBC/NoPadding", "HmacSHA256");
        } catch (RuntimeException failure) {
            drm.closeSession(sessionId);
            throw failure;
        }
    }

    synchronized byte[] createKeyRequest(byte[] initialization) throws Exception {
        requireOpen();
        if (requested) throw new IllegalStateException("Exchange already started");
        if (initialization == null || initialization.length == 0)
            throw new IllegalArgumentException("Missing init data");
        // Request session-only operator keys. These are separate from a
        // movie license and should not outlive this diagnostic session.
        byte[] request = drm.getKeyRequest(sessionId, initialization, "application/xml",
                MediaDrm.KEY_TYPE_STREAMING, new java.util.HashMap<>()).getData();
        if (request.length == 0) throw new IllegalStateException("Empty CDM request");
        requested = true;
        return request;
    }

    /** Input is the decoded MSL keydata response, not software keys. */
    synchronized void acceptKeyResponse(byte[] response, byte[] encryptionId, byte[] signingId)
            throws Exception {
        requireOpen();
        if (!requested || encryptionKeyId != null)
            throw new IllegalStateException("No pending exchange");
        if (response == null || response.length == 0 || encryptionId == null
                || encryptionId.length == 0 || signingId == null || signingId.length == 0)
            throw new IllegalArgumentException("Incomplete key response");
        // Ready only after the CDM has accepted the protected response.
        offlineKeySetId = drm.provideKeyResponse(sessionId, response);
        encryptionKeyId = encryptionId.clone();
        hmacKeyId = signingId.clone();
    }

    /** Caller supplies padded plaintext and the IV from the MSL envelope. */
    synchronized byte[] encryptBlocks(byte[] plaintext, byte[] iv) {
        requireKeys();
        requireBlocks(plaintext, iv);
        return crypto.encrypt(encryptionKeyId, plaintext, iv);
    }

    synchronized byte[] sign(byte[] authenticatedEnvelope) {
        requireKeys();
        return crypto.sign(hmacKeyId, authenticatedEnvelope);
    }

    synchronized boolean verify(byte[] authenticatedEnvelope, byte[] signature) {
        requireKeys();
        return crypto.verify(hmacKeyId, authenticatedEnvelope, signature);
    }

    synchronized byte[] encryptEnvelope(byte[] message) throws Exception {
        requireKeys();
        int padding = 16 - message.length % 16;
        byte[] padded = Arrays.copyOf(message, message.length + padding);
        Arrays.fill(padded, message.length, padded.length, (byte) padding);
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        try {
            byte[] ciphertext = crypto.encrypt(encryptionKeyId, padded, iv);
            return new JSONObject().put("version", 1)
                    .put("keyid", Base64.encodeToString(encryptionKeyId, Base64.NO_WRAP))
                    .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                    .put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                    .put("sha256", "AA==").toString().getBytes(StandardCharsets.UTF_8);
        } finally {
            Arrays.fill(padded, (byte) 0);
        }
    }

    synchronized byte[] decryptVerifiedEnvelope(byte[] envelope, byte[] signature) throws Exception {
        requireKeys();
        // Verify exactly the bytes that will be parsed, before any decryption.
        if (!crypto.verify(hmacKeyId, envelope, signature))
            throw new SecurityException("MSL signature mismatch");
        JSONObject parsed = new JSONObject(new String(envelope, StandardCharsets.UTF_8));
        // V1 envelopes may omit version (MSL's legacy/default representation).
        if (parsed.optInt("version", 1) != 1) throw new IllegalArgumentException("Envelope version");
        byte[] iv = Base64.decode(parsed.getString("iv"), Base64.DEFAULT);
        byte[] ciphertext = Base64.decode(parsed.getString("ciphertext"), Base64.DEFAULT);
        requireBlocks(ciphertext, iv);
        byte[] plaintext = crypto.decrypt(encryptionKeyId, ciphertext, iv);
        try {
            if (plaintext.length == 0) throw new SecurityException("Empty CBC response");
            int padding = plaintext[plaintext.length - 1] & 255;
            if (padding < 1 || padding > 16 || padding > plaintext.length)
                throw new SecurityException("Invalid CBC padding");
            for (int i = plaintext.length - padding; i < plaintext.length; i++)
                if ((plaintext[i] & 255) != padding) throw new SecurityException("Invalid CBC padding");
            return Arrays.copyOf(plaintext, plaintext.length - padding);
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("Session closed");
    }

    private void requireKeys() {
        requireOpen();
        if (encryptionKeyId == null || hmacKeyId == null)
            throw new IllegalStateException("Exchange incomplete");
    }

    private static void requireBlocks(byte[] input, byte[] iv) {
        if (iv == null || iv.length != 16 || input == null || input.length == 0
                || input.length % 16 != 0)
            throw new IllegalArgumentException("Invalid CBC blocks");
    }

    @Override public synchronized void close() throws Exception {
        if (closed) return;
        closed = true;
        try {
            try {
                if (offlineKeySetId != null && offlineKeySetId.length > 0
                        && android.os.Build.VERSION.SDK_INT >= 29) {
                    try {
                        drm.removeOfflineLicense(offlineKeySetId);
                    } catch (Exception missingProvision) {
                        // Some vendor JNI implementations throw this checked
                        // exception although removeOfflineLicense omits it.
                        if (!(missingProvision instanceof NotProvisionedException)) throw missingProvision;
                        provisioner.provision();
                        drm.removeOfflineLicense(offlineKeySetId);
                    }
                    android.util.Log.i("DRBEPNativeCDM", "offline_test_license_removed=true");
                }
            } finally {
                drm.closeSession(sessionId);
            }
        } finally {
            Arrays.fill(sessionId, (byte) 0);
            if (encryptionKeyId != null) Arrays.fill(encryptionKeyId, (byte) 0);
            if (hmacKeyId != null) Arrays.fill(hmacKeyId, (byte) 0);
            if (offlineKeySetId != null) Arrays.fill(offlineKeySetId, (byte) 0);
            offlineKeySetId = null;
            encryptionKeyId = null;
            hmacKeyId = null;
        }
    }
}
