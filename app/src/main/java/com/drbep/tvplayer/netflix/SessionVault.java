package com.drbep.tvplayer.netflix;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Account cookies encrypted with an app-scoped, non-exported Android Keystore key. */
final class SessionVault {
    static Map<String, String> load(Context context, String identity) throws Exception {
        String record = context.getSharedPreferences("netflix_account", Context.MODE_PRIVATE)
                .getString("session", null);
        if (record == null) throw new IllegalStateException("Account session missing");
        if (record.length() > 128 * 1024) throw new SecurityException("Oversized session");
        JSONObject envelope = new JSONObject(record);
        if (envelope.getInt("version") != 1) throw new SecurityException("Unknown session format");
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey key = (SecretKey) store.getKey("drbep-netflix-account-v1", null);
        if (key == null) throw new IllegalStateException("Session key missing");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128,
                Base64.decode(envelope.getString("iv"), Base64.DEFAULT)));
        byte[] clear = cipher.doFinal(Base64.decode(envelope.getString("ciphertext"), Base64.DEFAULT));
        try {
            JSONObject data = new JSONObject(new String(clear, StandardCharsets.UTF_8));
            if (identity == null || !identity.equals(data.getString("identity")))
                throw new SecurityException("Session belongs to another installation");
            Map<String, String> cookies = new java.util.HashMap<>();
            for (String name : new String[] {"NetflixId", "SecureNetflixId", "nfvdid"}) {
                String value = data.getString(name);
                if (value.isEmpty() || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0
                        || value.indexOf(';') >= 0) throw new SecurityException("Invalid stored cookie");
                cookies.put(name, value);
            }
            return cookies;
        } finally { java.util.Arrays.fill(clear, (byte) 0); }
    }

    static void save(Context context, String identity, Map<String, String> cookies) throws Exception {
        JSONObject data = new JSONObject().put("identity", identity)
                .put("created", System.currentTimeMillis());
        for (String name : new String[] {"NetflixId", "SecureNetflixId", "nfvdid"}) {
            String value = cookies.get(name);
            if (value == null || value.isEmpty()) throw new IllegalStateException("Incomplete account session");
            data.put(name, value);
        }
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        String alias = "drbep-netflix-account-v1";
        if (!store.containsAlias(alias)) {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(alias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            generator.generateKey();
        }
        SecretKey key = (SecretKey) store.getKey(alias, null);
        byte[] clear = data.toString().getBytes(StandardCharsets.UTF_8);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(clear);
            Cipher check = Cipher.getInstance("AES/GCM/NoPadding");
            check.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, cipher.getIV()));
            byte[] restored = check.doFinal(encrypted);
            try {
                if (!java.util.Arrays.equals(clear, restored)) throw new SecurityException("Vault verification failed");
            } finally { java.util.Arrays.fill(restored, (byte) 0); }
            String record = new JSONObject().put("version", 1)
                    .put("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .put("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP)).toString();
            if (!context.getSharedPreferences("netflix_account", Context.MODE_PRIVATE).edit()
                    .putString("session", record).commit()) throw new java.io.IOException("Session save failed");
        } finally { java.util.Arrays.fill(clear, (byte) 0); }
    }
}
