package com.drbep.tvplayer;

import org.json.*;
import java.util.*;

/** Read-only projection of the TV's currently visible channels and existing EPG repository. */
final class ServerGuidePanel {
    static String manage(JSONObject r,List<ChannelItem> channels,EpgRepository repository,TimelineEpgStore store)throws Exception {
        String action=r.getString("action");int offset=Math.max(0,r.optInt("offset"));long now=System.currentTimeMillis();
        JSONObject out=new JSONObject().put("feed","Guía de TV · plataforma actual").put("mode","server");
        if(action.equals("guide-channels")){
            String q=r.optString("query").toLowerCase(Locale.ROOT);JSONArray rows=new JSONArray();int skip=0;
            for(ChannelItem c:channels){if(!c.name.toLowerCase(Locale.ROOT).contains(q))continue;if(skip++<offset)continue;
                rows.put(new JSONObject().put("id",c.id).put("name",c.name).put("epgId",c.tvgId).put("occurrence",0));if(rows.length()==51)break;
            }return out.put("channels",rows).toString();
        }
        if(!action.equals("guide-day")&&!action.equals("guide-detail"))throw new IllegalArgumentException("Read only");
        ChannelItem channel=null;for(ChannelItem c:channels)if(c.id.equals(r.getString("identity"))){channel=c;break;}
        if(channel==null)throw new IllegalArgumentException("Channel no longer visible");
        long[] range=PlaylistGuidePanel.dayRange(r.getString("day"));
        if(range[0]<now-9L*86400000||range[0]>now+15L*86400000)throw new IllegalArgumentException("Day outside guide window");
        List<EpgRepository.EpgProgram> programmes=store.read(channel,range[0],range[1]);
        if(programmes.isEmpty()){
            List<EpgRepository.EpgProgram> fetched=range[1]<now?repository.fetchPastChannelPrograms(channel,400,8):repository.fetchChannelPrograms(channel,400);
            store.save(channel,fetched);programmes=store.read(channel,range[0],range[1]);
        }
        JSONArray rows=new JSONArray();int skip=0;
        for(EpgRepository.EpgProgram p:programmes){
            if(skip++<offset)continue;long start=EpgTimeCodec.parseEpochMillis(p.startTime),end=EpgTimeCodec.parseEpochMillis(p.endTime);
            if(action.equals("guide-detail")&&start!=r.getLong("start"))continue;
            JSONObject row=new JSONObject().put("start",start).put("end",end).put("title",p.title).put("meta",p.category).put("color",ProgrammePresentation.color(p.category)).put("state",ProgrammePresentation.state(start,end,now));
            if(action.equals("guide-detail"))row.put("description",p.description).put("image",p.icon).put("credits",new JSONObject());rows.put(row);if(rows.length()==51)break;
        }
        return out.put("programmes",rows).put("epgId",channel.tvgId).put("now",now).put("timeZone",TimeZone.getDefault().getID()).toString();
    }
}
