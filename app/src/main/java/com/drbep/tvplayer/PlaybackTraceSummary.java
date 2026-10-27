package com.drbep.tvplayer;

import org.json.*;
import java.util.*;

/** Reconstructs only recorded phases of the last eight globally identified playback attempts. */
final class PlaybackTraceSummary {
    private final LinkedHashMap<String,JSONObject> traces=new LinkedHashMap<>();
    void accept(JSONObject event) throws JSONException {
        String kind=event.optString("event"),session=event.optString("session");
        long op=event.optLong("op"),elapsed=event.optLong("elapsed",-1);
        if(session.isEmpty() || op<=0 || elapsed<0)return;
        String key=session+"|"+op;
        if(kind.equals("PLAY_PREPARE") && event.optLong("c")==1) {
            JSONObject trace=new JSONObject().put("operation",op).put("at",event.optLong("at"))
                    .put("started",elapsed).put("stages",new JSONObject().put("PREPARE",0L));
            traces.put(key,trace);
            while(traces.size()>8)traces.remove(traces.keySet().iterator().next());
            return;
        }
        JSONObject trace=traces.get(key);
        if(trace==null || elapsed<trace.getLong("started"))return;
        String stage=null;
        if(kind.equals("PLAY_STATE")) {
            if(event.optLong("a")==2)stage="BUFFERING";
            if(event.optLong("a")==3)stage="READY";
        } else if(kind.equals("PLAY_FRAME") && event.optLong("c")==0) {
            stage="FRAME";trace.put("preview",event.optLong("b")==1);
        } else if(kind.equals("SURFACE_FRAME"))stage="SURFACE";
        else if(kind.equals("PLAY_ERROR")){stage="ERROR";trace.put("error",event.optLong("a"));}
        if(kind.equals("PLAY_MANIFEST_LOADED"))stage="MANIFEST";
        else if(kind.equals("PLAY_MEDIA_LOADED"))stage="MEDIA";
        else if(kind.equals("PLAY_VIDEO_DECODER"))stage="VIDEO_DECODER";
        if(stage!=null) {
            JSONObject stages=trace.getJSONObject("stages");
            if((stage.equals("MANIFEST")||stage.equals("MEDIA")||stage.equals("VIDEO_DECODER"))
                    && !stages.has(stage)) {
                if(stages.has("FRAME") || event.optLong("a",-1)<0 || event.optLong("b",-1)<0)return;
                JSONObject details=trace.optJSONObject("details");
                if(details==null){details=new JSONObject();trace.put("details",details);}
                details.put(stage,new JSONObject().put("durationMs",event.optLong("a"))
                        .put("bytes",event.optLong("b")).put("trackType",event.optLong("c")));
            }
            if(!stages.has(stage))stages.put(stage,elapsed-trace.getLong("started"));
        }
    }
    JSONArray json() throws JSONException {
        JSONArray out=new JSONArray();
        for(JSONObject trace:traces.values()){
            JSONObject copy=new JSONObject(trace.toString());copy.remove("started");out.put(copy);
        }
        return out;
    }
}
