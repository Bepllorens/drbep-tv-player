package com.drbep.tvplayer;
import org.json.*;
import java.util.*;

/** Explicit portable UI preferences only; never credentials, server identities or parental controls. */
final class PreferencesBackupCodec {
 static final String WIDTH = "menu_width_percent";
 static final String HIDE = "menu_hide_seconds";
 static final String PINNED = "settings_pinned_ids";
 static final Set<String> KEYS = new HashSet<>(Arrays.asList(
   "startup_hub_disabled", "playback_repair_enabled", "ui_palette", "playback_quality_mode", WIDTH, HIDE, PINNED));
 static String encode(Map<String, ?> all) throws JSONException {
  JSONObject values = new JSONObject();
  for (String key : KEYS) {
   Object value = all.get(key);
   if (value != null) values.put(key, value instanceof Set ? new JSONArray((Set<?>)value) : value);
  }
  JSONObject result = new JSONObject().put("format", "drbep.preferences").put("version", 1).put("settings", values);
  decode(result.toString());
  return result.toString(2);
 }
 static Map<String, Object> decode(String json) throws JSONException {
  if (json == null || json.length() > 65536) throw new IllegalArgumentException("Copia demasiado grande");
  JSONObject root = new JSONObject(json);
  if (!"drbep.preferences".equals(root.optString("format")) || !(root.opt("version") instanceof Integer) || root.getInt("version") != 1)
   throw new IllegalArgumentException("Formato de copia no compatible");
  JSONObject values = root.getJSONObject("settings");
  Map<String, Object> result = new LinkedHashMap<>();
  Iterator<String> it = values.keys();
  while (it.hasNext()) {
   String key = it.next(); Object v = values.get(key);
   if (!KEYS.contains(key)) throw new IllegalArgumentException("La copia contiene una preferencia no permitida");
   boolean valid;
   switch(key) {
    case "startup_hub_disabled": case "playback_repair_enabled": valid = v instanceof Boolean; break;
    case "ui_palette": valid = v instanceof String && Arrays.asList("aurora","graphite","emerald","high-contrast").contains(v); break;
    case "playback_quality_mode": valid = v instanceof String && Arrays.asList("auto","data_saver","high").contains(v); break;
    case WIDTH: valid = v instanceof Integer && Arrays.asList(85,100,115).contains(v); break;
    case HIDE: valid = v instanceof Integer && Arrays.asList(0,3,5,8,12).contains(v); break;
    case PINNED:
     valid = v instanceof JSONArray && ((JSONArray)v).length() <= 40;
     Set<String> pins = new LinkedHashSet<>();
     if (valid) for (int i=0;i<((JSONArray)v).length();i++) {
      Object item = ((JSONArray)v).get(i);
      if (!(item instanceof String) || !((String)item).matches("[a-z0-9-]{1,80}")) {valid=false;break;}
      pins.add((String)item);
     }
     v=pins;break;
    default: valid=false;
   }
   if (!valid) throw new IllegalArgumentException("Valor no válido para " + key);
   result.put(key,v);
  }
  if (result.isEmpty()) throw new IllegalArgumentException("La copia no contiene preferencias");
  return result;
 }
}
