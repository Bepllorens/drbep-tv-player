package com.drbep.tvplayer;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Converts declarative playlist metadata; never executes Kodi request/response templates. */
final class PlaylistPlaybackConfig {
    String url, scheme = "", license = "", mime = "";
    final Map<String,String> streamHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    final Map<String,String> licenseHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static PlaylistPlaybackConfig parse(String rawUrl, JSONObject directives) throws Exception {
        PlaylistPlaybackConfig c = new PlaylistPlaybackConfig();
        String[] stream = rawUrl.split("\\|",2); c.url = http(stream[0]);
        String type = directives.optString("kodiprop:inputstream.adaptive.manifest_type").toLowerCase(Locale.ROOT);
        if(type.equals("mpd")) c.mime="application/dash+xml";
        else if(type.equals("hls") || type.equals("m3u8")) c.mime="application/x-mpegURL";
        else if(type.equals("ism") || type.equals("smooth")) c.mime="application/vnd.ms-sstr+xml";
        else if(!type.isEmpty()) throw new IllegalArgumentException("Tipo de manifiesto no compatible");
        header(c.streamHeaders,"User-Agent",directives.optString("extvlcopt:http-user-agent"));
        header(c.streamHeaders,"Referer",directives.optString("extvlcopt:http-referrer"));
        if(directives.has("http-json")) {
            JSONObject h = new JSONObject(directives.getString("http-json"));
            Iterator<String> keys = h.keys();
            while(keys.hasNext()) { String k=keys.next(); header(c.streamHeaders,k,h.getString(k)); }
        }
        pairs(c.streamHeaders,directives.optString("kodiprop:inputstream.adaptive.stream_headers"));
        if(stream.length>1) pairs(c.streamHeaders,stream[1]);
        String drm=directives.optString("kodiprop:inputstream.adaptive.license_type").toLowerCase(Locale.ROOT);
        String key=directives.optString("kodiprop:inputstream.adaptive.license_key");
        if(drm.isEmpty() && key.isEmpty()) return c;
        if(drm.contains("widevine")) c.scheme="widevine";
        else if(drm.contains("clearkey")) c.scheme="clearkey";
        else throw new IllegalArgumentException("Sistema DRM no compatible en esta lista");
        if(key.isEmpty()) throw new IllegalArgumentException("Falta la configuración de licencia");
        if(c.scheme.equals("clearkey") && !key.startsWith("http://") && !key.startsWith("https://")) {
            JSONObject jwk;
            if(key.trim().startsWith("{")) {
                jwk = new JSONObject(key);
                if (!jwk.has("keys")) {
                    // Kodi also accepts a JSON object mapping hexadecimal KIDs to keys.
                    JSONObject mapping = jwk;
                    JSONArray mappedKeys = new JSONArray();
                    Iterator<String> ids = mapping.keys();
                    while (ids.hasNext()) {
                        String id = ids.next();
                        mappedKeys.put(new JSONObject().put("kty", "oct")
                                .put("kid", hex(id)).put("k", hex(mapping.getString(id))));
                    }
                    jwk = new JSONObject().put("keys", mappedKeys).put("type", "temporary");
                }
                JSONArray keys=jwk.getJSONArray("keys");
                if(keys.length()==0 || keys.length()>64) throw new IllegalArgumentException("ClearKey inválido");
                for(int i=0;i<keys.length();i++) {
                    JSONObject k=keys.getJSONObject(i);
                    if(!"oct".equals(k.getString("kty")) || Base64.getUrlDecoder().decode(k.getString("kid")).length!=16 || Base64.getUrlDecoder().decode(k.getString("k")).length!=16) throw new IllegalArgumentException("ClearKey inválido");
                }
            } else {
                JSONArray keys=new JSONArray();
                for(String pair:key.split(",")) {
                    String[] parts=pair.trim().split(":",-1);
                    if(parts.length!=2) throw new IllegalArgumentException("ClearKey inválido");
                    keys.put(new JSONObject().put("kty","oct").put("kid",hex(parts[0])).put("k",hex(parts[1])));
                }
                jwk=new JSONObject().put("keys",keys).put("type","temporary");
            }
            c.license="data:application/json;base64,"+Base64.getEncoder().encodeToString(jwk.toString().getBytes(StandardCharsets.UTF_8));
        } else {
            String[] parts=key.split("\\|",-1); c.license=http(parts[0]);
            if(parts.length>4 || (parts.length>2 && !parts[2].isEmpty() && !parts[2].equals("R{SSM}")) || (parts.length>3 && !parts[3].isEmpty() && !parts[3].equals("B"))) throw new IllegalArgumentException("Plantilla de licencia Kodi no compatible");
            if(parts.length>1) pairs(c.licenseHeaders,parts[1]);
        }
        return c;
    }
    private static String http(String value) throws Exception {
        URI uri=new URI(value.trim());
        if(uri.getHost()==null || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) throw new IllegalArgumentException("Se requiere una URL HTTP o HTTPS");
        return value.trim();
    }
    private static void pairs(Map<String,String> out,String value) throws Exception {
        if(value.isEmpty()) return;
        for(String pair:value.split("&")) {
            String[] p=pair.split("=",2);
            if(p.length!=2) throw new IllegalArgumentException("Cabecera mal formada");
            header(out,URLDecoder.decode(p[0],"UTF-8"),URLDecoder.decode(p[1],"UTF-8"));
        }
    }
    private static void header(Map<String,String> out,String name,String value) {
        if(value.isEmpty()) return;
        if(!name.matches("[A-Za-z0-9!#$%&'*+.^_`~-]+") || value.contains("\r") || value.contains("\n") || name.equalsIgnoreCase("Host") || name.equalsIgnoreCase("Content-Length") || name.equalsIgnoreCase("Transfer-Encoding")) throw new IllegalArgumentException("Cabecera no permitida");
        out.put(name,value);
    }
    private static String hex(String s) {
        if(!s.matches("[a-fA-F0-9]{32}")) throw new IllegalArgumentException("ClearKey inválido");
        byte[] b=new byte[16]; for(int i=0;i<16;i++) b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
