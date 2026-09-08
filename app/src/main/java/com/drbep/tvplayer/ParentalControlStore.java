package com.drbep.tvplayer;

import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class ParentalControlStore {
    private static final long DEFAULT_UNLOCK_DURATION_MS = 15L * 60L * 1000L;
    private static final String HASH_PREFIX = "pbkdf2";
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int PBKDF2_KEY_BITS = 256;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long FAILED_ATTEMPT_LOCK_MS = 30_000L;

    private final SharedPreferences prefs;
    private final String prefPinHash;
    private final String prefPinSalt;
    private final String prefUnlockedUntilMs;
    private final String prefFailedAttempts;
    private final String prefBlockedUntilMs;

    private String pinHash = "";
    private String pinSalt = "";
    private long unlockedUntilMs;
    private int failedAttempts;
    private long blockedUntilMs;

    ParentalControlStore(SharedPreferences prefs, String prefix) {
        this.prefs = prefs;
        String safePrefix = prefix == null ? "parental" : prefix.trim();
        this.prefPinHash = safePrefix + "_pin_hash";
        this.prefPinSalt = safePrefix + "_pin_salt";
        this.prefUnlockedUntilMs = safePrefix + "_unlocked_until_ms";
        this.prefFailedAttempts = safePrefix + "_failed_attempts";
        this.prefBlockedUntilMs = safePrefix + "_blocked_until_ms";
        if (prefs != null) {
            this.pinHash = prefs.getString(prefPinHash, "");
            this.pinSalt = prefs.getString(prefPinSalt, "");
            this.unlockedUntilMs = prefs.getLong(prefUnlockedUntilMs, 0L);
            this.failedAttempts = prefs.getInt(prefFailedAttempts, 0);
            this.blockedUntilMs = prefs.getLong(prefBlockedUntilMs, 0L);
        }
    }

    boolean hasPinConfigured() {
        return !pinHash.trim().isEmpty() && !pinSalt.trim().isEmpty();
    }

    boolean isUnlocked() {
        return hasPinConfigured() && System.currentTimeMillis() < unlockedUntilMs;
    }

    long getUnlockedRemainingMs() {
        return Math.max(0L, unlockedUntilMs - System.currentTimeMillis());
    }

    void clearPin() {
        pinHash = "";
        pinSalt = "";
        unlockedUntilMs = 0L;
        if (prefs != null) {
            prefs.edit()
                    .remove(prefPinHash)
                    .remove(prefPinSalt)
                    .remove(prefUnlockedUntilMs)
                    .remove(prefFailedAttempts)
                    .remove(prefBlockedUntilMs)
                    .apply();
        }
    }

    void setPin(String pin) {
        String normalized = normalizePin(pin);
        if (!isValidPin(normalized)) {
            throw new IllegalArgumentException("invalid pin");
        }
        String nextSalt = generateSalt();
        String nextHash = pbkdf2Hash(nextSalt, normalized);
        pinSalt = nextSalt;
        pinHash = nextHash;
        unlockedUntilMs = 0L;
        failedAttempts = 0;
        blockedUntilMs = 0L;
        if (prefs != null) {
            prefs.edit()
                    .putString(prefPinHash, pinHash)
                    .putString(prefPinSalt, pinSalt)
                    .putLong(prefUnlockedUntilMs, unlockedUntilMs)
                    .putInt(prefFailedAttempts, 0)
                    .putLong(prefBlockedUntilMs, 0L)
                    .apply();
        }
    }

    boolean verifyPin(String pin) {
        if (!hasPinConfigured()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now < blockedUntilMs) {
            return false;
        }
        String normalized = normalizePin(pin);
        if (!isValidPin(normalized)) {
            recordFailedAttempt(now);
            return false;
        }
        boolean legacyHash = !pinHash.startsWith(HASH_PREFIX + "$");
        String candidate = legacyHash ? sha256Hex(pinSalt + ":" + normalized) : pbkdf2Hash(pinSalt, normalized);
        boolean verified = MessageDigest.isEqual(
                pinHash.getBytes(StandardCharsets.UTF_8),
                candidate.getBytes(StandardCharsets.UTF_8)
        );
        if (!verified) {
            recordFailedAttempt(now);
            return false;
        }
        clearFailedAttempts();
        if (legacyHash) {
            setPin(normalized);
        }
        return true;
    }

    long getBlockedRemainingMs() {
        return Math.max(0L, blockedUntilMs - System.currentTimeMillis());
    }

    void unlockSession() {
        unlockSession(DEFAULT_UNLOCK_DURATION_MS);
    }

    void unlockSession(long durationMs) {
        if (!hasPinConfigured()) {
            return;
        }
        unlockedUntilMs = System.currentTimeMillis() + Math.max(60_000L, durationMs);
        if (prefs != null) {
            prefs.edit().putLong(prefUnlockedUntilMs, unlockedUntilMs).apply();
        }
    }

    void lockSession() {
        unlockedUntilMs = 0L;
        if (prefs != null) {
            prefs.edit().putLong(prefUnlockedUntilMs, 0L).apply();
        }
    }

    static boolean isValidPin(String pin) {
        String normalized = normalizePin(pin);
        if (normalized.length() < 4 || normalized.length() > 6) {
            return false;
        }
        for (int i = 0; i < normalized.length(); i++) {
            if (!Character.isDigit(normalized.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    static String normalizePin(String pin) {
        return pin == null ? "" : pin.trim();
    }

    private static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return bytesToHex(salt);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("sha-256 unavailable", e);
        }
    }

    private static String pbkdf2Hash(String saltHex, String pin) {
        char[] chars = normalizePin(pin).toCharArray();
        byte[] salt = hexToBytes(saltHex);
        try {
            byte[] derived;
            try {
                derived = derivePbkdf2("PBKDF2WithHmacSHA256", chars, salt);
            } catch (Exception unavailable) {
                derived = derivePbkdf2("PBKDF2WithHmacSHA1", chars, salt);
            }
            return HASH_PREFIX + "$" + PBKDF2_ITERATIONS + "$" + bytesToHex(derived);
        } catch (Exception e) {
            throw new IllegalStateException("pbkdf2 unavailable", e);
        } finally {
            java.util.Arrays.fill(chars, '\0');
        }
    }

    private static byte[] derivePbkdf2(String algorithm, char[] pin, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(pin, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS);
        return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded();
    }

    private void recordFailedAttempt(long now) {
        failedAttempts++;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            blockedUntilMs = now + FAILED_ATTEMPT_LOCK_MS;
            failedAttempts = 0;
        }
        if (prefs != null) {
            prefs.edit()
                    .putInt(prefFailedAttempts, failedAttempts)
                    .putLong(prefBlockedUntilMs, blockedUntilMs)
                    .apply();
        }
    }

    private void clearFailedAttempts() {
        failedAttempts = 0;
        blockedUntilMs = 0L;
        if (prefs != null) {
            prefs.edit()
                    .putInt(prefFailedAttempts, 0)
                    .putLong(prefBlockedUntilMs, 0L)
                    .apply();
        }
    }

    private static byte[] hexToBytes(String value) {
        String clean = value == null ? "" : value.trim();
        if ((clean.length() & 1) != 0) {
            throw new IllegalArgumentException("invalid hex salt");
        }
        byte[] out = new byte[clean.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        char[] out = new char[bytes.length * 2];
        final char[] alphabet = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            out[i * 2] = alphabet[value >>> 4];
            out[i * 2 + 1] = alphabet[value & 0x0f];
        }
        return new String(out);
    }
}
