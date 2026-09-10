package com.drbep.tvplayer;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** User-scoped HBO progress. Only editorial metadata is persisted or synchronized. */
final class HboWatchHistory {
    static final class Entry {
        String id, title, kind, seriesId, seriesTitle;
        int season, episode;
        long duration, position, updated;
        boolean completed, pending, nextResolved;
        Entry(JSONObject j) {
            id=j.optString("id"); title=j.optString("title"); kind=j.optString("kind", "movie");
            seriesId=j.optString("series_id"); seriesTitle=j.optString("series_title");
            season=j.optInt("season"); episode=j.optInt("episode");
            duration=Math.max(0,j.optLong("duration_seconds")); position=Math.max(0,j.optLong("position_ms"));
            updated=j.optLong("updated_at"); completed=j.optBoolean("completed"); pending=j.optBoolean("pending"); nextResolved=j.optBoolean("next_resolved");
        }
        JSONObject json() {
            try { return new JSONObject().put("id",id).put("title",title).put("kind",kind)
                .put("series_id",seriesId).put("series_title",seriesTitle).put("season",season).put("episode",episode)
                .put("duration_seconds",duration).put("position_ms",position).put("updated_at",updated)
                .put("completed",completed).put("pending",pending).put("next_resolved",nextResolved); } catch(Exception e){ throw new IllegalStateException(e); }
        }
        String group(){return seriesId.isEmpty()?id:"series:"+seriesId;}
        String label(){return kind.equals("episode")?seriesTitle+" · T"+season+" · E"+episode+" — "+title:title;}
    }
    private final Map<String,Entry> entries=new LinkedHashMap<>();
    private final Set<String> watched=new LinkedHashSet<>();
    synchronized void load(String raw) {
        entries.clear(); watched.clear();
        try {JSONObject root=new JSONObject(raw); JSONArray a=root.optJSONArray("entries");
            if(a!=null) for(int i=0;i<a.length() && i<500;i++){Entry e=new Entry(a.getJSONObject(i));if(valid(e))entries.put(e.id,e);}
            JSONArray w=root.optJSONArray("watched"); if(w!=null)for(int i=0;i<w.length()&&i<2000;i++)watched.add(w.optString(i));
        }catch(Exception ignored){}
    }
    synchronized String save(){try{JSONArray a=new JSONArray();for(Entry e:entries.values())a.put(e.json());
        return new JSONObject().put("entries",a).put("watched",new JSONArray(watched)).toString();
    }catch(Exception e){throw new IllegalStateException(e);}}
    private static boolean valid(Entry e){return e.id.matches("[A-Za-z0-9_-]{1,180}")&&!e.title.isEmpty()&&(e.kind.equals("movie")||e.kind.equals("episode"));}
    synchronized Entry get(String id){return entries.get(rawId(id));}
    static String rawId(String id){return id!=null&&id.startsWith("hbomax:")?id.substring(7):id;}
    synchronized void remember(JSONObject metadata){Entry e=new Entry(metadata);if(!valid(e))return;
        Entry old=entries.get(e.id);if(old!=null){e.position=old.position;e.updated=old.updated;e.completed=old.completed;e.pending=old.pending;e.nextResolved=old.nextResolved;if(e.duration==0)e.duration=old.duration;}
        entries.put(e.id,e); trim();}
    private void trim(){if(entries.size()>500){List<Entry> a=new ArrayList<>(entries.values());a.sort(Comparator.comparingLong(e->e.updated));for(int i=0;i<a.size()-500;i++)entries.remove(a.get(i).id);}}
    synchronized boolean record(String id,long position,long duration,long now){Entry e=get(id);if(e==null||position<=0||e.completed)return false;
        if(duration>60000)e.duration=duration/1000;
        e.position=position; e.updated=now; e.pending=false;
        boolean ended=e.duration>60&&position>=e.duration*1000d*.98;
        boolean newly=ended&&!e.completed;e.completed=ended;
        if(ended)watched.add("hbomax:"+e.id);
        return newly;
    }
    synchronized void restart(String id){Entry e=get(id);if(e!=null){e.position=0;e.completed=false;e.pending=false;e.nextResolved=false;e.updated=System.currentTimeMillis();watched.remove("hbomax:"+e.id);}}
    synchronized void next(String completedId,JSONObject metadata){Entry previous=get(completedId);if(previous==null||!previous.completed)return;
        previous.nextResolved=true;
        if(metadata==null)return;
        Entry next=new Entry(metadata);if(!valid(next)||!previous.seriesId.equals(next.seriesId))return;
        // Do not replace a newer choice made while the catalog request was running.
        for(Entry e:entries.values())if(e.group().equals(previous.group())&&e.updated>previous.updated)return;
        Entry old=entries.get(next.id);if(old!=null&&old.completed)return;
        if(old!=null)next.position=old.position;
        next.pending=next.position==0;next.updated=previous.updated+1;entries.put(next.id,next);trim();
    }
    synchronized List<String> unresolved(){List<String> ids=new ArrayList<>();for(Entry e:entries.values())if(e.completed&&!e.nextResolved&&!e.seriesId.isEmpty()&&ids.size()<16)ids.add(e.id);return ids;}
    synchronized List<Entry> continuing(){Map<String,Entry> latest=new HashMap<>();
        for(Entry e:entries.values()){if(e.updated<=0)continue;Entry old=latest.get(e.group());if(old==null||e.updated>old.updated)latest.put(e.group(),e);}
        List<Entry> result=new ArrayList<>();for(Entry e:latest.values())if(!e.completed&&(e.pending||e.position>30000))result.add(e);
        result.sort((a,b)->Long.compare(b.updated,a.updated));return result;
    }
    synchronized String badge(String id){Entry e=get(id);if(watched.contains("hbomax:"+rawId(id))||(e!=null&&e.completed))return "Visto";
        if(e==null)return "";if(e.pending)return "Siguiente episodio";return e.position>30000?"Continuar · "+e.position/60000+" min":"";}
    synchronized void exportInto(JSONObject payload){try{
        JSONObject progress=payload.getJSONObject("vod_progress"), series=payload.getJSONObject("series_continuity");
        Set<String> allWatched=new LinkedHashSet<>();JSONArray w=payload.optJSONArray("watched");if(w!=null)for(int i=0;i<w.length();i++)allWatched.add(w.optString(i));allWatched.addAll(watched);
        for(Entry e:entries.values()){
            String id="hbomax:"+e.id;progress.remove(id);
            if(!e.completed&&e.position>0&&e.duration>60)progress.put(id,new JSONObject().put("position",e.position/1000d).put("duration",e.duration).put("updated_at",e.updated).put("hbo",e.json()));
        }
        Map<String,Entry> latest=new HashMap<>();for(Entry e:entries.values())if(!e.seriesId.isEmpty()&&e.updated>0){Entry old=latest.get(e.seriesId);if(old==null||e.updated>old.updated)latest.put(e.seriesId,e);}
        for(Entry e:latest.values()){
            String key="hbomax:"+e.seriesId;
            JSONObject row=new JSONObject().put("identity",key).put("vodType","hbomax").put("updatedAt",e.updated);
            if(e.completed)row.put("deletedAt",e.updated);
            else row.put("title",e.seriesTitle).put("parent",new JSONObject().put("id",e.seriesId).put("title",e.seriesTitle).put("vodType","hbomax").put("seriesGroup",true))
                .put("next",new JSONObject().put("id","hbomax:"+e.id).put("title",e.title).put("seriesTitle",e.seriesTitle).put("seasonNum",e.season).put("episodeNum",e.episode).put("kind","episode").put("vodType","hbomax"));
            series.put(key,row);
        }
        payload.put("watched",new JSONArray(allWatched));
    }catch(Exception e){throw new IllegalStateException(e);}}
    synchronized void merge(JSONObject payload){try{
        JSONArray w=payload.optJSONArray("watched");if(w!=null)for(int i=0;i<w.length();i++){String id=w.optString(i);if(id.startsWith("hbomax:"))watched.add(id);}
        JSONObject p=payload.optJSONObject("vod_progress");if(p!=null){Iterator<String> ids=p.keys();while(ids.hasNext()){String id=ids.next();if(!id.startsWith("hbomax:"))continue;
            JSONObject row=p.optJSONObject(id);if(row==null||row.optJSONObject("hbo")==null)continue;
            Entry e=new Entry(row.getJSONObject("hbo"));e.position=Math.round(row.optDouble("position")*1000);e.duration=(long)row.optDouble("duration");e.updated=row.optLong("updated_at");e.completed=false;
            Entry old=entries.get(e.id);if(valid(e)&&(old==null||e.updated>old.updated))entries.put(e.id,e);
        }}
        JSONObject s=payload.optJSONObject("series_continuity");if(s!=null){Iterator<String> keys=s.keys();while(keys.hasNext()){String key=keys.next();if(!key.startsWith("hbomax:"))continue;
            JSONObject row=s.getJSONObject(key);long updated=row.optLong("updatedAt");String seriesId=key.substring(7);
            long local=0;for(Entry e:entries.values())if(e.seriesId.equals(seriesId))local=Math.max(local,e.updated);
            if(updated<local)continue;
            if(row.optLong("deletedAt")>0){for(Entry e:entries.values())if(e.seriesId.equals(seriesId)&&e.updated<=updated){e.completed=true;e.updated=updated;}continue;}
            JSONObject n=row.optJSONObject("next"),parent=row.optJSONObject("parent");if(n==null||parent==null)continue;
            String id=rawId(n.optString("id"));Entry e=entries.get(id);
            if(e==null)e=new Entry(new JSONObject().put("id",id).put("title",n.optString("title")).put("kind","episode").put("series_id",seriesId).put("series_title",parent.optString("title")).put("season",n.optInt("seasonNum")).put("episode",n.optInt("episodeNum")));
            if(!valid(e))continue;e.updated=updated;e.completed=false;e.pending=e.position==0;entries.put(id,e);
        }}trim();
    }catch(Exception ignored){} }
}
