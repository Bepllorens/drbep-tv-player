package com.drbep.tvplayer;

import org.json.*;
import java.util.*;

/** Bounded reconstruction from existing events, invoked only when diagnostics are read. */
final class StartupTraceSummary {
    private final ArrayDeque<JSONObject> launches=new ArrayDeque<>();
    private JSONObject current;
    private String session="";
    private long started;
    private static final Set<String> STAGES=new HashSet<>(Arrays.asList(
            "WINDOW","PLAYER_READY","STARTUP_FRAME","CATALOG_READY"));
    void accept(JSONObject event) throws JSONException {
        String kind=event.optString("event"),owner=event.optString("session");
        if(kind.equals("ACTIVITY")) {
            if(owner.isEmpty() || !event.has("elapsed"))return;
            session=owner;started=event.getLong("elapsed");
            current=new JSONObject().put("at",event.optLong("at")).put("version",event.optInt("version"))
                    .put("stages",new JSONObject());
            launches.add(current);if(launches.size()>6)launches.removeFirst();
        } else if(kind.equals("SESSION")) {
            current=null;session="";
        } else if(current!=null && STAGES.contains(kind) && session.equals(owner)
                && event.optLong("elapsed",-1)>=started) {
            long duration=event.optLong("a",-1);
            JSONObject stages=current.getJSONObject("stages");
            if(duration>=0 && !stages.has(kind))stages.put(kind,duration);
        }
    }
    JSONArray json() { return new JSONArray(launches); }
}
