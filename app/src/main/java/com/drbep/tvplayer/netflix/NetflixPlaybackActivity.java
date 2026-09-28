package com.drbep.tvplayer.netflix;
import android.app.Activity;
import android.os.Bundle;
import android.media.MediaDrm;
import android.media.NotProvisionedException;
import android.net.Uri;
import android.util.Log;
import java.util.UUID;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Account-linked Netflix playback through the native device CDM. */
public final class NetflixPlaybackActivity extends androidx.activity.ComponentActivity {
    private static final String TAG = "DRBEPNativeCDM";
    volatile Runnable stopPlayback;
    NetflixPlaybackControls controls;
    volatile boolean completed;
    private boolean failed;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE
                | android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        long title = getIntent().getLongExtra("manifest_title", 0);
        if (title <= 0) { showError("El título de Netflix no es válido."); return; }
        getIntent().putExtra("uhd", true);
        showLogin("Preparando Netflix…");
        new Thread(() -> {
            try (MediaDrm drm = new MediaDrm(UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"))) {
                if (!"AFTKRT".equals(android.os.Build.MODEL) || !"27925".equals(drm.getPropertyString("systemId")))
                    throw new IllegalStateException("Unsupported device");
                String identity = getPreferences(MODE_PRIVATE).getString("native_identity", null);
                if (identity == null) {
                    byte[] random = new byte[32]; new java.security.SecureRandom().nextBytes(random);
                    StringBuilder suffix = new StringBuilder();
                    for (byte b : random) suffix.append(String.format(java.util.Locale.ROOT, "%02X", b & 255));
                    identity = "NFANDROID2-PRV-FIRETVSTICK2023-AMAZOAFTKRT-27925-" + suffix;
                    if (!getPreferences(MODE_PRIVATE).edit().putString("native_identity", identity).commit()) throw new IllegalStateException("Identity not saved");
                    java.util.Arrays.fill(random, (byte)0);
                }
                boolean login;
                try { SessionVault.load(this, identity).clear(); login = false; }
                catch (Exception missing) { login = true; }
                if (login) showLogin("Vinculando Netflix…\nPrepara tu cuenta para introducir el código que aparecerá aquí.");
                try { byte[] check = drm.openSession(); drm.closeSession(check); }
                catch (NotProvisionedException e) { provision(drm); }
                try (NativeMslSession msl = new NativeMslSession(drm, () -> provision(drm))) {
                    byte[] challenge = msl.createKeyRequest(new byte[] {0x0a, 0x7a, 0, 0x6c, 0x38, 0x2b});
                    try { MslHandshake.exchange(msl, challenge, identity, login, this); }
                    finally { java.util.Arrays.fill(challenge, (byte)0); }
                }
                runOnUiThread(() -> { if (!failed && !isFinishing()) finish(); });
            } catch (Exception error) {
                Log.i(TAG, "netflix_playback_error=" + error.getClass().getSimpleName());
                showError("No se pudo iniciar Netflix. Puedes reintentar o volver a vincular tu cuenta.");
            }
        }, "netflix-native-playback").start();
    }
    @Override public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        if (controls != null && controls.key(event)) return true;
        return super.dispatchKeyEvent(event);
    }
    @Override public void onBackPressed() {
        if (controls != null) controls.back(); else exitPlayback(false);
    }
    void exitPlayback(boolean toTv) {
        setResult(RESULT_OK, new android.content.Intent().putExtra("return_to_tv", toTv));
        Runnable stop = stopPlayback;
        if (stop != null) stop.run(); else finish();
    }
    @Override protected void onStop() {
        Runnable stop = stopPlayback;
        if (stop != null) { stop.run(); finish(); }
        super.onStop();
    }
    @Override protected void onDestroy() {
        Runnable stop = stopPlayback; if (stop != null) stop.run();
        super.onDestroy();
    }
    void showLogin(String message) { showMessage(message, false); }
    void showError(String message) { failed = true; showMessage(message, true); }
    private void showMessage(String message, boolean actions) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
            layout.setOrientation(1); layout.setGravity(android.view.Gravity.CENTER);
            layout.setPadding(48,32,48,32); layout.setBackgroundColor(0xff141418);
            android.widget.TextView text = new android.widget.TextView(this);
            text.setText(message); text.setTextSize(25); text.setTextColor(0xffffffff); text.setGravity(17);
            layout.addView(text);
            if (actions) {
                android.widget.Button retry = new android.widget.Button(this); retry.setText("Reintentar");
                retry.setOnClickListener(v -> { startActivity(new android.content.Intent(getIntent())); finish(); }); layout.addView(retry);
                android.widget.Button link = new android.widget.Button(this); link.setText("Volver a vincular Netflix");
                link.setOnClickListener(v -> { getSharedPreferences("netflix_account",0).edit().remove("session").apply(); startActivity(new android.content.Intent(getIntent())); finish(); }); layout.addView(link);
                android.widget.Button tv = new android.widget.Button(this); tv.setText("Volver a TV");
                tv.setOnClickListener(v -> exitPlayback(true)); layout.addView(tv);
                retry.requestFocus();
            }
            setContentView(layout);
        });
    }
    private static void provision(MediaDrm drm) throws Exception {
        MediaDrm.ProvisionRequest request = drm.getProvisionRequest();
        Uri endpoint = Uri.parse(request.getDefaultUrl());
        String host = endpoint.getHost();
        if (!"https".equals(endpoint.getScheme()) || host == null
                || !(host.equals("googleapis.com") || host.endsWith(".googleapis.com")
                || host.equals("google.com") || host.endsWith(".google.com"))) {
            throw new SecurityException("Unexpected provisioning origin");
        }
        URL url = new URL(endpoint.buildUpon().appendQueryParameter("signedRequest",
                new String(request.getData(), StandardCharsets.UTF_8)).build().toString());
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(0);
        try {
            connection.getOutputStream().close();
            int status = connection.getResponseCode();
            Log.i(TAG, "provision_http=" + status);
            if (status != 200) throw new java.io.IOException("Provision rejected");
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (response.size() + count > 1024 * 1024)
                        throw new java.io.IOException("Oversized provision response");
                    response.write(buffer, 0, count);
                }
            }
            drm.provideProvisionResponse(response.toByteArray());
            Log.i(TAG, "provision_accepted=true");
        } finally {
            connection.disconnect();
        }
    }
}
