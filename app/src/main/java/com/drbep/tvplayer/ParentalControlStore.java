package com.drbep.tvplayer;

import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

final class ParentalControlStore {
    private static final long DEFAULT_UNLOCK_DURATION_MS = 15L * 60L * 1000L;

    private final SharedPreferences prefs;
    private final String prefPinHash;
    private final String prefPinSalt;
    private final String prefUnlockedUntilMs;

    private String pinHash = "";
    private String pinSalt = "";
    private long unlockedUntilMs;

    ParentalControlStore(SharedPreferences prefs, String prefix) {
        this.prefs = prefs;
        String safePrefix = prefix == null ? "parental" : prefix.trim();
        this.prefPinHash = safePrefix + "_pin_hash";
        this.prefPinSalt = safePrefix + "_pin_salt";
        this.prefUnlockedUntilMs = safePrefix + "_unlocked_until_ms";
        if (prefs != null) {
            this.pinHash = prefs.getString(prefPinHash, "");
            this.pinSalt = prefs.getString(prefPinSalt, "");
            this.unlockedUntilMs = prefs.getLong(prefUnlockedUntilMs, 0L);
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
                    .apply();
        }
    }

    void setPin(String pin) {
        String normalized = normalizePin(pin);
        if (!isValidPin(normalized)) {
            throw new IllegalArgumentException("invalid pin");
        }
        String nextSalt = generateSalt();
        String nextHash = sha256Hex(nextSalt + ":" + normalized);
        pinSalt = nextSalt;
        pinHash = nextHash;
        unlockedUntilMs = 0L;
        if (prefs != null) {
            prefs.edit()
                    .putString(prefPinHash, pinHash)
                    .putString(prefPinSalt, pinSalt)
                    .putLong(prefUnlockedUntilMs, unlockedUntilMs)
                    .apply();
        }
    }

    boolean verifyPin(String pin) {
        if (!hasPinConfigured()) {
            return false;
        }
        String normalized = normalizePin(pin);
        if (!isValidPin(normalized)) {
            return false;
        }
        return pinHash.equals(sha256Hex(pinSalt + ":" + normalized));
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
