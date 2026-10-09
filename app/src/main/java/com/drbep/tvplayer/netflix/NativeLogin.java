package com.drbep.tvplayer.netflix;

import android.net.Uri;
import android.os.Build;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

/** Initial TV-link negotiation. Uses only cookies minted for this installation. */
final class NativeLogin {
    private static final String TAG = "DRBEPNativeCDM";
    private static final String FAMILY = "NFANDROID2-PRV-FIRETVSTICK2023-AMAZOAFTKRT";

    static void initiate(String identity, Map<String, String> cookies, NetflixPlaybackActivity activity) throws Exception {
        checkOptionalTokenParsing();
        for (String name : new String[] {"NetflixId", "SecureNetflixId", "nfvdid"})
            if (!cookies.containsKey(name)) throw new IllegalStateException("Missing own device session");
        String operation = "clcsLegacyMoneyballInitiateSession";
        JSONArray arguments = new JSONArray();
        stringArgument(arguments, "deviceModel", Build.MODEL);
        stringArgument(arguments, "deviceName", Build.MODEL);
        stringArgument(arguments, "deviceTypeOverride", FAMILY);
        stringArgument(arguments, "esn", identity);
        stringArgument(arguments, "deviceLocale", "es-ES");
        stringArgument(arguments, "availableLocales", "es-ES,en-US");
        stringArgument(arguments, "application_name", "DRBEP");
        stringArgument(arguments, "application_v", "1");
        stringArgument(arguments, "appVersion", "1");
        stringArgument(arguments, "swVersion", Build.DISPLAY);
        stringArgument(arguments, "resolution", "720p");
        stringArgument(arguments, "sourceType", "2");
        // Advertise the TV-link flow implemented below (code + server confirmation).
        stringArgument(arguments, "secureVLV", "true");
        stringArgument(arguments, "dw", "1280");
        stringArgument(arguments, "dh", "720");
        booleanArgument(arguments, "hasGooglePlayServiceOnTenfoot", false);
        booleanArgument(arguments, "isSuspendedMode", false);
        booleanArgument(arguments, "fetchPartnerStrings", false);
        JSONObject variables = new JSONObject().put("action", "").put("flow", "tenfootSignUp")
                .put("hasGooglePlayService", false).put("imageFormat", "PNG")
                .put("inputFields", new JSONArray()).put("legacyRequestArguments", arguments)
                .put("mode", "none").put("resolutionMode", "TV_720P")
                .put("supportedVideoFormat", "mp4");
        JSONObject response = post(identity, cookies, operation,
                "5152154d-6b61-4333-a738-92dc4ab712bd", variables);
        JSONArray errors = response.optJSONArray("errors");
        if (errors != null && errors.length() > 0) {
            Log.i(TAG, "login_graphql_errors=" + errors.length());
            for (int i = 0; i < Math.min(errors.length(), 3); i++) {
                JSONObject error = errors.optJSONObject(i);
                if (error == null) continue;
                JSONObject extensions = error.optJSONObject("extensions");
                String code = extensions == null ? "" : extensions.optString("code");
                Log.i(TAG, "login_graphql_code="
                        + (code.matches("[A-Z0-9_]{1,64}") ? code : "unavailable"));
            }
            return;
        }
        JSONObject data = response.optJSONObject("data");
        JSONObject start = data == null ? null : data.optJSONObject(operation);
        boolean state = start != null && !start.optString("serverState").isEmpty();
        boolean flow = false;
        String flowSession = null;
        String initialMode = null;
        String initialFlow = null;
        if (start != null) {
            JSONObject tree = start.optJSONObject("componentTree");
            JSONArray nodes = tree == null ? null : tree.optJSONArray("nodes");
            if (nodes != null) for (int i = 0; i < nodes.length(); i++) {
                JSONObject node = nodes.optJSONObject(i);
                if (node == null || !node.has("payloadJson")) continue;
                JSONObject payload = new JSONObject(node.getString("payloadJson"));
                logFlowSchema(payload);
                JSONObject fields = payload.optJSONObject("fields");
                JSONObject signIn = fields == null ? null : fields.optJSONObject("signInAction");
                if (signIn != null) {
                    for (String name : new String[] {"mode", "flow", "fieldType"}) {
                        String value = signIn.optString(name);
                        if (value.matches("[A-Za-z][A-Za-z0-9_]{0,63}"))
                            Log.i(TAG, "login_signin_" + name + "=" + value);
                    }
                }
                if (!payload.optString("flwssn").isEmpty()) {
                    flow = true;
                    flowSession = payload.getString("flwssn");
                    initialMode = payload.optString("mode");
                    initialFlow = payload.optString("flow");
                }
            }
        }
        Log.i(TAG, "login_initiate_state=" + state + " flow=" + flow
                + " account_authenticated=false");
        if (!state || !flow) throw new IllegalStateException("Incomplete link state");
        if (initialMode == null || !initialMode.matches("[A-Za-z][A-Za-z0-9_]{0,63}")
                || !"tenfootSignUp".equals(initialFlow))
            throw new IllegalStateException("Unsupported returned login flow");
        JSONObject submit = post(identity, cookies, "clcsLegacyMoneyballSubmit",
                "e8ef3234-6525-4975-8796-1299602e3297", new JSONObject()
                .put("action", "signInAction").put("flow", initialFlow)
                .put("flwssn", flowSession).put("imageFormat", "PNG")
                .put("inputFields", new JSONArray()).put("mode", initialMode)
                .put("requestArguments", arguments).put("resolutionMode", "TV_720P")
                .put("serverState", start.getString("serverState")));
        JSONObject screen = submit.getJSONObject("data")
                .getJSONObject("clcsLegacyMoneyballSubmit").getJSONObject("screen");
        diagnoseResponse(submit, 0, new int[] {0});
        String serverState = screen.getString("serverState");
        String pin = null;
        JSONArray nodes = screen.getJSONObject("componentTree").getJSONArray("nodes");
        for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = nodes.optJSONObject(i);
            if (node != null && "CLCSPinText".equals(node.optString("__typename"))
                    && "login-qr-pin".equals(node.optString("testId"))) pin = node.getString("code");
        }
        String update = updateToken(screen.optJSONObject("onRender"));
        Log.i(TAG, "login_submit_shape pin=" + (pin != null)
                + " pin_format=" + (pin != null && pin.matches("[A-Za-z0-9 -]{4,16}"))
                + " update=" + (update != null) + " nodes=" + nodes.length());
        for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = nodes.optJSONObject(i);
            if (node != null && pin == null) {
                String type = node.optString("__typename");
                String test = node.optString("testId");
                Log.i(TAG, "login_screen_node=" + (type.matches("[A-Za-z]{1,64}") ? type : "unknown")
                        + " test=" + (test.matches("[A-Za-z0-9_-]{1,80}") ? test : "none"));
                if ("CLCSChipToggle".equals(type)) describeAction(node, "login_switch", 0);
            }
            if (node != null && "CLCSTvEnterCredentials".equals(node.optString("__typename"))
                    && pin == null) {
                Log.i(TAG, "login_code_unavailable=true credentials_screen=true web_escape="
                        + node.optBoolean("hasWebSignInEscapeHatch", false));
                JSONObject handlers = node.optJSONObject("handlers");
                JSONObject next = handlers == null ? null : handlers.optJSONObject("nextButtonAction");
                JSONArray effects = next == null ? null : next.optJSONArray("nodes");
                if (effects != null) for (int j = 0; j < effects.length(); j++) {
                    JSONObject effect = effects.optJSONObject(j);
                    if (effect == null) continue;
                    describeAction(effect, "next", 0);
                    String type = effect.optString("__typename");
                    Log.i(TAG, "login_next_effect="
                            + (type.matches("[A-Za-z]{1,64}") ? type : "unavailable")
                            + " server_action=" + effect.has("serverAction")
                            + " legacy_action=" + effect.has("action")
                            + " input_fields=" + effect.has("inputFields"));
                }
                activity.showLogin("Netflix no ha ofrecido un código de vinculación.\n\n"
                        + "La respuesta solicita correo y contraseña.\n"
                        + "Este prototipo todavía no implementa ese formulario.\n\n"
                        + "No se ha vinculado la cuenta. Atrás para salir.");
                return;
            }
        }
        if (pin == null || !pin.matches("[A-Za-z0-9 -]{4,16}") || update == null) {
            StringBuilder display = new StringBuilder("Netflix no ha devuelto un código.\n\n");
            for (int i = 0; i < nodes.length(); i++) {
                JSONObject node = nodes.optJSONObject(i);
                if (node == null || !"CLCSText".equals(node.optString("__typename"))) continue;
                for (String name : new String[] {"plainContent", "richContent"}) {
                    Object text = node.opt(name);
                    if (text instanceof JSONObject) text = ((JSONObject) text).opt("value");
                    if (text instanceof String && ((String) text).length() < 600)
                        display.append((String) text).append("\n");
                }
            }
            activity.showLogin(display.toString());
            Log.i(TAG, "login_alternate_screen=true no_poll_started=true");
            return;
        }
        Log.i(TAG, "login_activation_code_ready=true");
        activity.showLogin("Vincular Netflix con DRBEP\n\nAbre netflix.com/tv2\nCódigo: " + pin
                + "\n\nEsperando confirmación (máximo 5 minutos).\nAtrás para cancelar.");
        long deadline = android.os.SystemClock.elapsedRealtime() + 300000;
        boolean submitted = false;
        String deviceNetflixId = cookies.get("NetflixId");
        while (!activity.isDestroyed() && !activity.isFinishing()
                && android.os.SystemClock.elapsedRealtime() < deadline) {
            JSONObject poll = post(identity, cookies, "clcsScreenUpdate",
                    "8daa70b0-fc21-4b5e-8c7e-ce0f31c8ca66", new JSONObject()
                    .put("imageFormat", "PNG").put("inputFields", new JSONArray())
                    .put("resolutionMode", "TV_720P").put("serverState", serverState)
                    .put("serverScreenUpdate", update));
            JSONObject result = poll.getJSONObject("data").getJSONObject("clcsScreenUpdate");
            JSONObject effect = result.optJSONObject("effect");
            if (effect != null) {
                String next = updateToken(effect);
                if (next != null) update = next;
                JSONArray effects = effect.optJSONArray("nodes");
                if (effects != null) for (int i = 0; i < effects.length(); i++) {
                    JSONObject node = effects.optJSONObject(i);
                    if (node != null && "Submitted".equals(node.optString("loggingAction"))) submitted = true;
                }
            }
            JSONObject nextScreen = result.optJSONObject("screen");
            String returnedState = optionalToken(nextScreen, "serverState");
            if (returnedState != null) serverState = returnedState;
            boolean member = containsCurrentMember(nextScreen);
            Log.i(TAG, "login_poll_state screen=" + (nextScreen != null)
                    + " effect=" + (effect != null) + " member=" + member
                    + " cookies_changed=" + !deviceNetflixId.equals(cookies.get("NetflixId")));
            if (member && !deviceNetflixId.equals(cookies.get("NetflixId"))) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                SessionVault.save(activity, identity, cookies);
                activity.accountLinkConfirmed = true;
                Log.i(TAG, "login_account_confirmed=true session_saved=true");
                activity.showLogin("Cuenta Netflix vinculada.\n\nSesión guardada en este dispositivo."
                        + "\nPreparando reproducción…");
                return;
            }
            if (submitted) Log.i(TAG, "login_submission_seen=true account_confirmed=" + member);
            if (activity.isFinishing() || activity.isDestroyed()) return;
            Thread.sleep(5000);
        }
        Log.i(TAG, "login_wait_ended=true account_authenticated=false");
        activity.showLogin("Vinculación sin completar.\nEl código ya no se está comprobando.");
    }

    private static String updateToken(JSONObject container) {
        JSONArray nodes = container == null ? null : container.optJSONArray("nodes");
        if (nodes != null) for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = nodes.optJSONObject(i);
            String value = optionalToken(node, "serverScreenUpdate");
            if (value != null) return value;
        }
        return null;
    }

    private static String optionalToken(JSONObject object, String key) {
        Object value = object == null ? null : object.opt(key);
        if (!(value instanceof String)) return null;
        String token = (String) value;
        return token.trim().isEmpty() || "null".equals(token) ? null : token;
    }

    private static void checkOptionalTokenParsing() throws Exception {
        JSONObject absent = new JSONObject();
        JSONObject empty = new JSONObject().put("serverState", JSONObject.NULL);
        if (optionalToken(absent, "serverState") != null
                || optionalToken(empty, "serverState") != null
                || optionalToken(new JSONObject().put("serverState", 42), "serverState") != null)
            throw new AssertionError("Invalid optional state accepted");
        JSONObject effects = new JSONObject().put("nodes", new JSONArray()
                .put(new JSONObject().put("serverScreenUpdate", JSONObject.NULL))
                .put(new JSONObject().put("serverScreenUpdate", "local-test-token")));
        if (!"local-test-token".equals(updateToken(effects)))
            throw new AssertionError("Null effect masked valid update token");
        Log.i(TAG, "login_optional_token_checks_passed=true");
    }

    private static void logFlowSchema(JSONObject payload) {
        for (String name : new String[] {"mode", "flow"}) {
            String value = payload.optString(name);
            Log.i(TAG, "login_initial_" + name + "="
                    + (value.matches("[A-Za-z][A-Za-z0-9_]{0,63}") ? value : "unavailable"));
        }
        for (String name : new String[] {"fields", "actions"}) {
            JSONObject fields = payload.optJSONObject(name);
            if (fields == null) continue;
            java.util.Iterator<String> keys = fields.keys();
            int count = 0;
            while (keys.hasNext() && count++ < 60) {
                String key = keys.next();
                if (!key.matches("[A-Za-z][A-Za-z0-9_]{0,63}")) continue;
                JSONObject value = fields.optJSONObject(key);
                Log.i(TAG, "login_initial_" + name + "_field=" + key
                        + " action=" + (value != null && "Action".equals(value.optString("fieldType"))));
            }
        }
    }

    private static void describeAction(JSONObject object, String path, int depth) {
        if (depth > 3) return;
        java.util.Iterator<String> keys = object.keys();
        int count = 0;
        while (keys.hasNext() && count++ < 20) {
            String key = keys.next();
            if (!key.matches("[A-Za-z_]{1,50}")) continue;
            Object value = object.opt(key);
            Log.i(TAG, "login_action_field=" + path + "." + key + " object=" + (value instanceof JSONObject));
            if (value instanceof JSONObject) describeAction((JSONObject) value, path + "." + key, depth + 1);
            if (value instanceof JSONArray && depth < 3) {
                JSONArray list = (JSONArray) value;
                for (int i = 0; i < Math.min(list.length(), 4); i++) {
                    JSONObject item = list.optJSONObject(i);
                    if (item != null) describeAction(item, path + "." + key + "[]", depth + 1);
                }
            }
        }
    }

    private static void diagnoseResponse(Object value, int depth, int[] visited) {
        if (depth > 14 || ++visited[0] > 2000) return;
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            java.util.Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object child = object.opt(key);
                if ("errorCode".equals(key) || "internalCode".equals(key)) {
                    String code = String.valueOf(child);
                    if (code.matches("[A-Z0-9_.-]{1,48}")) Log.i(TAG, "login_server_error_code=" + code);
                }
                diagnoseResponse(child, depth + 1, visited);
            }
        } else if (value instanceof JSONArray) {
            JSONArray list = (JSONArray) value;
            for (int i = 0; i < list.length(); i++) diagnoseResponse(list.opt(i), depth + 1, visited);
        }
    }

    private static boolean containsCurrentMember(JSONObject screen) {
        JSONObject tree = screen == null ? null : screen.optJSONObject("componentTree");
        JSONArray nodes = tree == null ? null : tree.optJSONArray("nodes");
        if (nodes != null) for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = nodes.optJSONObject(i);
            if (node != null && "CURRENT_MEMBER".equals(node.optString("membershipStatus"))) return true;
        }
        return false;
    }

    private static JSONObject post(String identity, Map<String, String> cookies, String operation,
            String queryId, JSONObject variables) throws Exception {
        Uri uri = Uri.parse("https://nrdp.prod.cloud.netflix.com/graphql").buildUpon()
                .appendQueryParameter("device_type", FAMILY).appendQueryParameter("esn", identity)
                .appendQueryParameter("o", operation).build();
        byte[] body = new JSONObject().put("operationName", operation).put("variables", variables)
                .put("extensions", new JSONObject().put("persistedQuery", new JSONObject()
                        .put("id", queryId).put("version", 102)))
                .toString().getBytes(StandardCharsets.UTF_8);
        HttpsURLConnection connection = (HttpsURLConnection) new URL(uri.toString()).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(25000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "DRBEP-Offline-Native/1");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Accept-Language", "es-ES,en-US");
        connection.setRequestProperty("X-Netflix.context.operation-name", operation);
        // Select the CLCS UI protocol used by the response parser, not a
        // borrowed application version or another device's identity.
        connection.setRequestProperty("X-Netflix.context.ui-flavor", "photon");
        connection.setRequestProperty("X-Netflix.request.device-model", Build.MODEL);
        connection.setRequestProperty("X-Netflix.Request.NonJson.Headers", "true");
        StringBuilder cookieHeader = new StringBuilder();
        for (String name : new String[] {"NetflixId", "SecureNetflixId", "nfvdid", "netflix-mfa-nonce"}) {
            String value = cookies.get(name);
            if (value == null) continue;
            if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0
                    || value.indexOf(';') >= 0) throw new SecurityException("Invalid own cookie");
            if (cookieHeader.length() > 0) cookieHeader.append("; ");
            cookieHeader.append(name).append('=').append(value);
        }
        connection.setRequestProperty("Cookie", cookieHeader.toString());
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(body.length);
        try {
            try (OutputStream output = connection.getOutputStream()) { output.write(body); }
            int status = connection.getResponseCode();
            Log.i(TAG, "login_operation=" + operation + " http=" + status);
            String retryAfter = connection.getHeaderField("Retry-After");
            Log.i(TAG, "login_retry_after_present=" + (retryAfter != null));
            if (retryAfter != null) throw new java.io.IOException("Provider requests delay");
            if (status != 200) throw new java.io.IOException("Login endpoint rejected request");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (bytes.size() + count > 1024 * 1024)
                        throw new java.io.IOException("Login response too large");
                    bytes.write(buffer, 0, count);
                }
            }
            JSONObject result = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            for (Map.Entry<String, java.util.List<String>> entry : connection.getHeaderFields().entrySet()) {
                if (!"Set-Cookie".equalsIgnoreCase(entry.getKey())) continue;
                for (String line : entry.getValue()) for (java.net.HttpCookie cookie : java.net.HttpCookie.parse(line)) {
                    String name = cookie.getName();
                    if (!("NetflixId".equals(name) || "SecureNetflixId".equals(name)
                            || "nfvdid".equals(name) || "netflix-mfa-nonce".equals(name))) continue;
                    if (cookie.hasExpired() || cookie.getValue().isEmpty()) cookies.remove(name);
                    else cookies.put(name, cookie.getValue());
                }
            }
            return result;
        } finally {
            java.util.Arrays.fill(body, (byte) 0);
            connection.disconnect();
        }
    }

    private static void stringArgument(JSONArray arguments, String name, String value) throws Exception {
        arguments.put(new JSONObject().put("name", name)
                .put("value", new JSONObject().put("stringValue", value)));
    }
    private static void booleanArgument(JSONArray arguments, String name, boolean value) throws Exception {
        arguments.put(new JSONObject().put("name", name)
                .put("value", new JSONObject().put("booleanValue", value)));
    }
}
