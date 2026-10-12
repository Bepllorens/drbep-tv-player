package com.drbep.tvplayer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.drbep.tvplayer.playlists.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import okhttp3.*;
import org.json.JSONObject;

/** Beta entry point. All database/network work stays off the UI thread. */
final class PersonalPlaylistController {
    interface Host {
        void menu(String title, String message, List<String> labels, List<Runnable> actions, Runnable back);
        void input(java.util.function.BiConsumer<String,String> submit, Runnable back);
        void pickFile();
        String serverUrl();
        Map<String,String> serverHeaders();
        void visual(String title,List<VodVisualItemUiModel> cards,List<VodVisualActionUiModel> actions,Runnable back);
        void post(Runnable action);
        void status(String message);
        void play(ChannelItem item);
        default void webPlay(ChannelItem item,boolean resume){play(item);}
        void movies(String source, Runnable back);
        void series(String source, Runnable back);
    }
    private final Context context;
    private final Host host;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final PlaylistDatabase db;
    private volatile boolean closed, busy;
    private volatile okhttp3.Call activeCall;
    PersonalPlaylistController(Context context, Host host) {
        this.context = context.getApplicationContext(); this.host = host;
        db = PlaylistDatabase.getInstance(context);
        PlaylistRefreshWorker.schedule(context);
    }
    void close() { closed = true; okhttp3.Call call = activeCall; if (call != null) call.cancel(); worker.shutdownNow(); }
    private void post(Runnable action) { if (!closed) host.post(() -> { if (!closed) action.run(); }); }
    private void work(Runnable action) { if (!closed) worker.execute(action); }
    void show(Runnable back) {
        work(() -> {
            List<String> labels = new ArrayList<>(); List<Runnable> actions = new ArrayList<>();
            labels.add("Importaciones y progreso"); actions.add(() -> showJobs(back));
            labels.add("Actualizar listas visibles"); actions.add(() -> show(back));
            labels.add("Añadir por URL"); actions.add(() -> host.input(this::importUrl, () -> show(back)));
            labels.add("Importar archivo M3U"); actions.add(host::pickFile);
            labels.add("Desde mi servidor DRBEP"); actions.add(() -> showServer(back));
            try (Cursor c = db.sources()) {
                while (c.moveToNext()) {
                    String id = c.getString(0), name = c.getString(1);
                    labels.add(name+(c.getLong(3)==0?" · Importación en curso":"")); actions.add(() -> source(id,name,back));
                }
                post(() -> host.menu("Mis listas", "Listas personales · beta", labels, actions, back));
            } catch (Exception e) { error(); }
        });
    }
    void showKind(String kind,Runnable back) {
        if (kind.equals("movie")) { host.movies("",back); return; }
        if (kind.equals("series")) { host.series("",back); return; }
        work(() -> {
            List<String> labels=new ArrayList<>(); List<Runnable> actions=new ArrayList<>();
            try(Cursor c=db.sources()) {
                while(c.moveToNext()) {
                    String id=c.getString(0), name=c.getString(1); labels.add(name+(c.getLong(3)==0?" · Importación en curso":""));
                    actions.add(() -> { if(kind.equals("series")) series(id,name,"",() -> showKind(kind,back)); else page(id,name,kind,null,-1,() -> showKind(kind,back)); });
                }
                if(labels.isEmpty()) { labels.add("Añadir una lista"); actions.add(() -> show(back)); }
                post(() -> host.menu(kind.equals("series")?"Series · Mis listas":"Películas · Mis listas","Selecciona una lista",labels,actions,back));
            } catch(Exception e) { error(); }
        });
    }
    private void visual(String title,List<String> labels,List<Runnable> actions,List<String> posters,Runnable back) {
        List<VodVisualItemUiModel> cards=new ArrayList<>(); List<VodVisualActionUiModel> navigation=new ArrayList<>();
        for(int i=0;i<labels.size();i++) {
            if(i<posters.size()) cards.add(new VodVisualItemUiModel(labels.get(i),"","",posters.get(i),actions.get(i),null));
            else navigation.add(new VodVisualActionUiModel(labels.get(i),false,actions.get(i)));
        }
        post(() -> host.visual(title,cards,navigation,back));
    }
    private void showServer(Runnable back) {
        String base = host.serverUrl().replaceAll("/+$", "");
        Map<String,String> headers = new HashMap<>(host.serverHeaders());
        work(() -> {
            try {
                okhttp3.Request.Builder request = new Request.Builder().url(base+"/api/offline/playlists");
                for(Map.Entry<String,String> header:headers.entrySet()) request.header(header.getKey(),header.getValue());
                try(okhttp3.Response response=NetworkClients.withTimeouts(15000,30000).newBuilder().followRedirects(false).followSslRedirects(false).build().newCall(request.build()).execute()) {
                    if(response.code()==404) { post(() -> host.status("El servidor todavía no tiene habilitadas las listas personales.")); return; }
                    if(!response.isSuccessful() || response.body()==null) throw new IOException("Server rejected request");
                    JSONObject payload;
                    try(InputStream input=new HttpClient.LimitedInputStream(response.body().byteStream(),1024*1024)) {
                        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream(); byte[] buffer=new byte[8192]; int n;
                        while((n=input.read(buffer))!=-1) bytes.write(buffer,0,n);
                        payload=new JSONObject(bytes.toString("UTF-8"));
                    }
                    org.json.JSONArray sources=payload.getJSONArray("sources");
                    List<String> labels=new ArrayList<>(); List<Runnable> actions=new ArrayList<>();
                    for(int i=0;i<Math.min(200,sources.length());i++) {
                        JSONObject source=sources.getJSONObject(i); String remote=source.getString("id"), name=source.getString("name");
                        long version=source.optLong("version");
                        if(!remote.matches("[a-f0-9]{32}")) continue;
                        labels.add(name+" · "+source.optLong("entries")+" elementos");
                        actions.add(() -> queueServer(name,base+":"+payload.optLong("account_id")+":"+remote));
                    }
                    post(() -> host.menu("Listas del servidor",labels.isEmpty()?"No tienes listas asignadas":"Selecciona una lista para importar o actualizar",labels,actions,() -> show(back)));
                }
            } catch(Exception e) { error(); }
        });
    }
    private void acknowledge(String base,String id,long version,String state,Map<String,String> headers) {
        try {
            JSONObject body=new JSONObject().put("version",version).put("state",state);
            Request.Builder request=new Request.Builder().url(base+"/api/offline/playlists/"+id+"/status").post(RequestBody.create(MediaType.parse("application/json"),body.toString()));
            for(Map.Entry<String,String> h:headers.entrySet())request.header(h.getKey(),h.getValue());
            try(okhttp3.Response response=NetworkClients.withTimeouts(5000,5000).newBuilder().followRedirects(false).followSslRedirects(false).build().newCall(request.build()).execute()) { /* Reporting never invalidates a committed import. */ }
        }catch(Exception ignored){}
    }
    private void source(String id, String name, Runnable back) {
        List<String> labels = Arrays.asList("Películas", "Series", "TV en directo", "Sin clasificar", "Actualizar lista", "Actualizar guía", "Eliminar lista");
        List<Runnable> actions = Arrays.asList(
            () -> host.movies(id, () -> source(id,name,back)),
            () -> host.series(id, () -> source(id,name,back)),
            () -> page(id,name,"live",null,-1, () -> source(id,name,back)),
            () -> page(id,name,"unknown",null,-1, () -> source(id,name,back)),
            () -> refreshSource(id,false),
            () -> refreshSource(id,true),
            () -> host.menu("Eliminar «"+name+"»", "Se eliminará esta lista del dispositivo.", Arrays.asList("Cancelar","Eliminar"), Arrays.asList(() -> source(id,name,back), () -> work(() -> {
                try { db.deleteSource(id); post(() -> show(back)); } catch (Exception e) { error(); }
            })), () -> source(id,name,back)));
        host.menu(name,"",labels,actions,() -> show(back));
    }
    private void series(String id, String name, String after, Runnable back) {
        work(() -> {
            List<String> labels = new ArrayList<>(); List<Runnable> actions = new ArrayList<>(); List<String> posters = new ArrayList<>();
            try (Cursor c = db.series(id,after,51)) {
                String last = after;
                while (c.moveToNext()) {
                    if (labels.size() == 50) { String next = last; labels.add("Más series →"); actions.add(() -> series(id,name,next, () -> series(id,name,after,back))); break; }
                    String title = c.getString(0); last = title; posters.add(c.getString(2));
                    labels.add(title+" · "+c.getInt(1)+" episodios");
                    actions.add(() -> page(id,title,"series",title,-1, () -> series(id,name,after,back)));
                }
                visual("Series · "+name,labels,actions,posters,back);
            } catch (Exception e) { error(); }
        });
    }
    private void page(String id,String title,String kind,String series,long after,Runnable back) {
        work(() -> {
            List<String> labels = new ArrayList<>(); List<Runnable> actions = new ArrayList<>(); List<String> posters = new ArrayList<>();
            try (Cursor c = series == null ? db.browse(id,kind,"",false,false,(int)(after+1),51) : db.episodes(id,series,after,51)) {
                long last = after;
                while (c.moveToNext()) {
                    if (labels.size()==50) { long next=after + 50; labels.add("Más →"); actions.add(() -> page(id,title,kind,series,next, () -> page(id,title,kind,series,after,back))); break; }
                    last = c.getLong(c.getColumnIndexOrThrow("position"));
                    ChannelItem item = item(c,title);
                    JSONObject attributes = new JSONObject(c.getString(c.getColumnIndexOrThrow("attributes")));
                    String label = c.getColumnIndex("display_title") >= 0 ? c.getString(c.getColumnIndexOrThrow("display_title")) : item.name;
                    labels.add(label); posters.add(item.logoUrl);
                    actions.add(() -> {
                        if (!item.playlistPlaybackError.isEmpty()) host.status(item.playlistPlaybackError);
                        else if (kind.equals("unknown")) host.status("La entrada no declara type=movie, series o live.");
                        else if (okhttp3.HttpUrl.parse(item.playUrl) == null) host.status("Esta beta admite enlaces de vídeo HTTP y HTTPS.");
                        else if(kind.equals("live")) host.menu(item.name,"",Arrays.asList("Ver canal","Guía y programas anteriores"),Arrays.asList(() -> host.play(item),() -> guide(id,item,attributes,0,back)),back);
                        else host.play(item);
                    });
                }
                if (kind.equals("movie") || kind.equals("series")) visual(title,labels,actions,posters,back);
                else post(() -> host.menu(title,labels.isEmpty()?"Sin contenido":"",labels,actions,back));
            } catch (Exception e) { error(); }
        });
    }
    private void guide(String source,ChannelItem channel,JSONObject attributes,int offset,Runnable back) {
        work(() -> {
            List<String> labels=new ArrayList<>();List<Runnable> actions=new ArrayList<>();long now=System.currentTimeMillis();
            try(Cursor c=db.guide(source,db.resolveGuideChannel(source,channel.tvgId,channel.name),now-7*86400000L,now+86400000L,offset)) {
                while(c.moveToNext()) {
                    if(labels.size()==50){labels.add("Más programas →");actions.add(() -> guide(source,channel,attributes,offset+50,() -> guide(source,channel,attributes,offset,back)));break;}
                    long start=c.getLong(c.getColumnIndexOrThrow("start_ms")),end=c.getLong(c.getColumnIndexOrThrow("end_ms"));String title=c.getString(c.getColumnIndexOrThrow("title"));
                    String date=new java.text.SimpleDateFormat("EEE dd/MM · HH:mm",java.util.Locale.getDefault()).format(new java.util.Date(start));
                    labels.add(date+" · "+title);
                    final String detailText=ProgrammePresentation.metadata(new JSONObject(c.getString(c.getColumnIndexOrThrow("metadata"))))+"\n\n"+c.getString(c.getColumnIndexOrThrow("description"));
                    Runnable replayAction=() -> {
                        try {
                            String url=PlaylistCatchup.url(channel.playUrl,attributes,start,end,System.currentTimeMillis());
                            ChannelItem replay=new ChannelItem(channel.id+":catchup:"+start,title,channel.tvgId,channel.logoUrl,channel.group,url,"",0,0,true,channel.isAdultVod,channel.platformId,channel.platformName,channel.customGroups,channel.drmScheme,channel.drmLicenseUrl,"m3u",true);
                            replay.playlistStreamHeaders=channel.playlistStreamHeaders;replay.playlistLicenseHeaders=channel.playlistLicenseHeaders;replay.playlistMimeType=channel.playlistMimeType;
                            host.play(replay);
                        } catch(Exception e) {host.status("Este programa no tiene catch-up disponible en la lista.");}
                    };
                    actions.add(() -> host.menu(title,detailText,Arrays.asList("Ver desde el principio"),Arrays.asList(replayAction),() -> guide(source,channel,attributes,offset,back)));
                }
                post(() -> host.menu("Guía · "+channel.name,labels.isEmpty()?"Sin programas. Actualiza la guía y comprueba el tvg-id de la lista.":"Programas ordenados por día y hora",labels,actions,back));
            }catch(Exception e){error();}
        });
    }
    ChannelItem item(Cursor c,String name) throws Exception {
        String source=c.getString(c.getColumnIndexOrThrow("source"));
        String identity=c.getString(c.getColumnIndexOrThrow("identity"));
        String kind=c.getString(c.getColumnIndexOrThrow("kind"));
        JSONObject a=new JSONObject(c.getString(c.getColumnIndexOrThrow("attributes")));
        String group=c.getString(c.getColumnIndexOrThrow("group_name"));
        PlaylistPlaybackConfig config = null;
        String error = "";
        try { config = PlaylistPlaybackConfig.parse(c.getString(c.getColumnIndexOrThrow("url")), new JSONObject(c.getString(c.getColumnIndexOrThrow("directives")))); }
        catch (Exception e) { error = "Configuración de reproducción no compatible o incompleta en esta entrada."; }
        ChannelItem item=new ChannelItem("m3u:"+source+":"+identity+":"+c.getInt(c.getColumnIndexOrThrow("occurrence")),
            c.getString(c.getColumnIndexOrThrow("title")),a.optString("tvg-id"),c.getString(c.getColumnIndexOrThrow("logo")),group,
            config == null ? "" : config.url,"",0,0,!kind.equals("live"),PrivateChannelPolicy.adultLabel(group) || PrivateChannelPolicy.adultLabel(c.getString(c.getColumnIndexOrThrow("title"))),0,name,new ArrayList<>(),config == null ? "" : config.scheme,config == null ? "" : config.license,"m3u",true);
        item.playlistPlaybackError = error;
        if (config != null) { item.playlistStreamHeaders = config.streamHeaders; item.playlistLicenseHeaders = config.licenseHeaders; item.playlistMimeType = config.mime; }
        item.vodSeriesTitle=c.getString(c.getColumnIndexOrThrow("series"));
        item.vodSeason=c.getInt(c.getColumnIndexOrThrow("season")); item.vodEpisode=c.getInt(c.getColumnIndexOrThrow("episode"));
        return item;
    }
    String manageWeb(String text) throws Exception {
        JSONObject request=new JSONObject(text);String action=request.optString("action"),id=request.optString("source");JSONObject result=new JSONObject();
        if(action.equals("import-url")) {
            String name=request.optString("name").trim(),url=request.optString("url").trim(),epg=request.optString("epg").trim();
            okhttp3.HttpUrl parsed=okhttp3.HttpUrl.parse(url);
            if(name.isEmpty()||name.length()>160||url.length()>8192||parsed==null||epg.length()>8192||(!epg.isEmpty()&&okhttp3.HttpUrl.parse(epg)==null))throw new IOException("Invalid source");
            String source=db.createSource(name,"url");
            try {
                db.configure(source,name,parsed.toString(),request.optBoolean("automatic"));
                if(!epg.isEmpty()){db.setGuideUrl(source,epg);db.guideMode(source,"manual");}
                return result.put("job",PlaylistImportJobs.enqueue(context,source,"url","")).put("queued",true).toString();
            }catch(Exception e){db.deleteUnpublishedSource(source);throw e;}
        }
        if(action.equals("jobs"))return result.put("jobs",PlaylistImportJobs.list(context)).toString();
        if(action.equals("cancel-job")){PlaylistImportJobs.cancel(context,request.getString("job"));return result.put("ok",true).toString();}
        if(action.equals("lists")) {
            org.json.JSONArray rows=new org.json.JSONArray();
            try(Cursor c=db.sources()){while(c.moveToNext())rows.put(new JSONObject().put("id",c.getString(0)).put("name",c.getString(1)).put("origin",c.getString(2)).put("updated",c.getLong(3)));}
            return result.put("sources",rows).toString();
        }
        try(Cursor c=db.source(id)){if(!c.moveToFirst())throw new IOException("Unknown source");}
        if(action.startsWith("guide-")) return new PlaylistGuidePanel(db).manage(request);
        if(action.equals("source-info")) {
            try(Cursor c=db.source(id)){c.moveToFirst();return result.put("automatic",c.getInt(c.getColumnIndexOrThrow("auto_refresh"))!=0).toString();}
        }
        if(action.equals("series")||action.equals("episodes")||action.equals("watched")||action.equals("clear-history")||action.equals("play-entry"))return libraryWeb(request);
        if(action.equals("entries")) {
            org.json.JSONArray rows=new org.json.JSONArray();String query=request.optString("query");if(query.length()>200)throw new IOException("Query too long");
            try(Cursor c=db.browse(id,request.optString("kind"),query,request.optBoolean("favorites"),request.optBoolean("hidden"),request.optInt("offset"),50)) {
                while(c.moveToNext())rows.put(new JSONObject().put("identity",c.getString(c.getColumnIndexOrThrow("identity"))).put("occurrence",c.getInt(c.getColumnIndexOrThrow("occurrence")))
                    .put("title",c.getString(c.getColumnIndexOrThrow("display_title"))).put("order",c.isNull(c.getColumnIndexOrThrow("sort_order"))?0:c.getInt(c.getColumnIndexOrThrow("sort_order"))).put("kind",c.getString(c.getColumnIndexOrThrow("kind"))).put("favorite",c.getInt(c.getColumnIndexOrThrow("favorite"))!=0).put("hidden",c.getInt(c.getColumnIndexOrThrow("hidden"))!=0));
            }
            return result.put("entries",rows).toString();
        }
        if(action.equals("preference")) {
            String identity=request.getString("identity"),label=request.optString("label");if(!identity.matches("[a-f0-9]{64}")||label.length()>300)throw new IOException("Invalid entry");
            int occurrence=Math.max(0,request.optInt("occurrence"));int order=request.optInt("order");
            try(Cursor c=db.preferenceState(id,identity,occurrence)){if(c.moveToFirst()){if(!request.has("label"))label=c.getString(0);if(!request.has("order"))order=c.getInt(1);}}
            db.preference(id,identity,occurrence,request.optBoolean("favorite"),request.optBoolean("hidden"),label,order);
        } else if(action.equals("delete")) { if(busy)throw new IOException("Import running");db.deleteSource(id);
        } else if(action.equals("configure")) {
            try(Cursor c=db.source(id)){c.moveToFirst();String name=request.optString("name",c.getString(c.getColumnIndexOrThrow("name"))).trim();String url=request.optString("url",c.getString(c.getColumnIndexOrThrow("locator")));
                if(name.isEmpty()||name.length()>160||(!url.isEmpty()&&okhttp3.HttpUrl.parse(url)==null))throw new IOException("Invalid source");
                String epg=request.has("epg")?request.getString("epg"):null;
                if(epg!=null&&!epg.isEmpty()&&okhttp3.HttpUrl.parse(epg)==null)throw new IOException("Invalid EPG URL");
                if("server".equals(c.getString(c.getColumnIndexOrThrow("origin")))&&!url.isEmpty())throw new IOException("Server lists are managed by the server");
                db.configure(id,name,url,request.optBoolean("automatic"));
                if(epg!=null){db.setGuideUrl(id,epg);db.guideMode(id,epg.isEmpty()?"off":"manual");}
            }
        } else if(action.equals("refresh")) { refreshSource(id,false);
        } else if(action.equals("guide")) { refreshSource(id,true);
        } else throw new IOException("Unknown action");
        return result.put("ok",true).toString();
    }
    java.util.Set<String> seriesIds(String source,String series) {
        java.util.Set<String> ids=new java.util.HashSet<>();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT identity,occurrence FROM entries WHERE source=? AND series=? AND kind='series'",new String[]{source,series})){while(c.moveToNext())ids.add("m3u:"+source+":"+c.getString(0)+":"+c.getInt(1));}
        return ids;
    }
    private String libraryWeb(JSONObject request)throws Exception {
        String action=request.getString("action"),source=request.getString("source"),series=request.optString("series");
        if(series.length()>300)throw new IOException("Invalid series");
        android.database.sqlite.SQLiteDatabase sql=db.getWritableDatabase();
        String from=" FROM entries e JOIN sources s ON s.id=e.source AND COALESCE(s.active,s.preview)=e.generation LEFT JOIN preferences p ON p.source=e.source AND p.identity=e.identity AND p.occurrence=e.occurrence ";
        JSONObject out=new JSONObject();org.json.JSONArray rows=new org.json.JSONArray();int offset=Math.max(0,request.optInt("offset"));
        if(action.equals("series")) {
            String query=request.optString("query");if(query.length()>200)throw new IOException("Invalid search");
            try(Cursor c=sql.rawQuery("SELECT e.series,COUNT(*) AS count"+from+" WHERE e.source=? AND e.kind='series' AND COALESCE(p.hidden,0)=0 AND instr(lower(e.series),lower(?))>0 GROUP BY e.series ORDER BY e.series COLLATE NOCASE LIMIT 51 OFFSET ?",new String[]{source,query,Integer.toString(offset)})){while(c.moveToNext())rows.put(new JSONObject().put("title",c.getString(0)).put("episodes",c.getInt(1)));}
            return out.put("rows",rows).toString();
        }
        if(action.equals("episodes")) {
            String order=request.optBoolean("descending")?" DESC":" ASC";
            org.json.JSONArray seasons=new org.json.JSONArray();
            try(Cursor c=sql.rawQuery("SELECT DISTINCT e.season"+from+" WHERE e.source=? AND e.kind='series' AND e.series=? AND COALESCE(p.hidden,0)=0 ORDER BY e.season"+order,new String[]{source,series})){while(c.moveToNext())seasons.put(c.getInt(0));}
            int season=request.has("season")?request.getInt("season"):seasons.optInt(0);
            try(Cursor c=sql.rawQuery("SELECT e.identity,e.occurrence,e.title,e.episode,COALESCE(p.resume_ms,0),COALESCE(p.watched,0)"+from+" WHERE e.source=? AND e.kind='series' AND e.series=? AND e.season=? AND COALESCE(p.hidden,0)=0 AND (?='0' OR COALESCE(p.watched,0)=0) ORDER BY e.episode"+order+",e.position LIMIT 51 OFFSET ?",new String[]{source,series,Integer.toString(season),request.optBoolean("hideWatched")?"1":"0",Integer.toString(offset)})){while(c.moveToNext())rows.put(new JSONObject().put("identity",c.getString(0)).put("occurrence",c.getInt(1)).put("title",c.getString(2)).put("episode",c.getInt(3)).put("position",c.getLong(4)).put("watched",c.getInt(5)!=0));}
            return out.put("rows",rows).put("seasons",seasons).put("season",season).toString();
        }
        if(action.equals("clear-history")) {
            sql.execSQL("UPDATE preferences SET resume_ms=0,watched=0 WHERE source=? AND EXISTS(SELECT 1 FROM entries e JOIN sources s ON s.id=e.source AND COALESCE(s.active,s.preview)=e.generation WHERE e.source=preferences.source AND e.identity=preferences.identity AND e.occurrence=preferences.occurrence AND e.kind='series' AND e.series=?)",new Object[]{source,series});return out.put("ok",true).toString();
        }
        String identity=request.getString("identity");int occurrence=request.optInt("occurrence");if(!identity.matches("[a-f0-9]{64}")||occurrence<0)throw new IOException("Invalid episode");
        try(Cursor c=sql.rawQuery("SELECT e.*"+from+" WHERE e.source=? AND e.identity=? AND e.occurrence=? AND COALESCE(p.hidden,0)=0 LIMIT 1",new String[]{source,identity,Integer.toString(occurrence)})){
            if(!c.moveToFirst())throw new IOException("Content no longer available");
            if(action.equals("watched")) {
                if(!"series".equals(c.getString(c.getColumnIndexOrThrow("kind"))))throw new IOException("Not an episode");
                sql.execSQL("INSERT OR IGNORE INTO preferences(source,identity,occurrence) VALUES(?,?,?)",new Object[]{source,identity,occurrence});
                sql.execSQL("UPDATE preferences SET watched=? WHERE source=? AND identity=? AND occurrence=?",new Object[]{request.optBoolean("watched")?1:0,source,identity,occurrence});
            }else{ChannelItem channel=item(c,"Mis listas");boolean resume=request.optBoolean("resume",true);host.post(()->host.webPlay(channel,resume));}
        }
        return out.put("ok",true).toString();
    }
    private void refreshSource(String id,boolean guide) {
        queue(() -> PlaylistImportJobs.enqueue(context,id,guide?"guide":"url",""));
    }
    private interface Enqueue {String run()throws Exception;}
    private void queue(Enqueue enqueue) {
        host.status("Importación en cola. Puedes seguir usando DRBEP.");
        PlaylistImportJobs.STAGING.execute(() -> {try{enqueue.run();}catch(Exception e){error();}});
    }
    private void queueServer(String name,String identity) {
        queue(() -> PlaylistImportJobs.enqueue(context,db.serverSource(identity,name),"server",""));
    }
    void showJobs(Runnable back) {
        work(() -> {
            try {
                org.json.JSONArray jobs=PlaylistImportJobs.list(context);List<String> labels=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
                labels.add("Actualizar progreso");actions.add(() -> showJobs(back));
                for(int i=0;i<jobs.length();i++) {
                    JSONObject j=jobs.getJSONObject(i);String id=j.getString("id"),state=j.getString("state");
                    boolean active=state.equals("queued")||state.equals("running")||state.equals("retry");
                    labels.add(j.getString("name")+" · "+j.getString("phase")+" · "+j.getLong("count")+(active?" · Cancelar":""));
                    actions.add(() -> {if(active)work(() -> {try{PlaylistImportJobs.cancel(context,id);post(() -> showJobs(back));}catch(Exception e){error();}});});
                }
                post(() -> host.menu("Importaciones", "Puedes explorar los primeros resultados desde Mis listas. OK en una tarea activa para cancelarla.",labels,actions,back));
            }catch(Exception e){error();}
        });
    }
    static InputStream openSource(String url) throws Exception {
        okhttp3.Response response=NetworkClients.withTimeouts(15000,30000).newCall(new Request.Builder().url(url).build()).execute();
        if(!response.isSuccessful()||response.body()==null){response.close();throw new IOException("Download failed");}
        InputStream raw=new FilterInputStream(response.body().byteStream()){@Override public void close()throws IOException{try{super.close();}finally{response.close();}}};
        java.io.PushbackInputStream peek=new java.io.PushbackInputStream(raw,2);int a=peek.read(),b=peek.read();if(b>=0)peek.unread(b);if(a>=0)peek.unread(a);
        InputStream decoded=a==31&&b==139?new java.util.zip.GZIPInputStream(peek):peek;
        return new HttpClient.LimitedInputStream(decoded,256*1024*1024);
    }
    synchronized boolean beginImport() { if (busy || closed) return false; busy=true; return true; }
    long importWeb(InputStream input) throws Exception {
        if (!beginImport()) throw new IOException("Another import is running");
        String id=null;
        try {
            id=db.createSource("Lista web", "file");
            M3uStreamReader.Result result=db.importReader(id,new InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8));
            post(() -> host.status("Lista web importada · " + result.imported + " elementos"));
            return result.imported;
        } catch(Exception e) {
            if(id!=null) db.deleteUnpublishedSource(id);
            throw e;
        } finally { busy=false; }
    }
    void importFile(Uri uri) {
        try { context.getContentResolver().takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch(Exception ignored) {}
        host.status("Preparando archivo para importar en segundo plano…");
        PlaylistImportJobs.STAGING.execute(() -> {
            String file=null,id=null;
            try(InputStream input=context.getContentResolver().openInputStream(uri)) {
                if(input==null)throw new IOException("Missing file");file=PlaylistImportJobs.stage(context,input);
                id=db.createSource("Archivo M3U","file");PlaylistImportJobs.enqueue(context,id,"file",file);
            }catch(Exception e){if(file!=null)new File(PlaylistImportJobs.spoolDir(context),file).delete();if(id!=null)db.deleteUnpublishedSource(id);error();}
            finally {try{context.getContentResolver().releasePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}}
        });
    }
    private void importUrl(String name,String url) {
        HttpUrl parsed=HttpUrl.parse(url.trim());if(parsed==null){host.status("Introduce una URL HTTP o HTTPS válida");return;}
        queue(() -> {String id=db.createSource(name.trim().isEmpty()?parsed.host():name.trim(),"url");db.configure(id,name.trim().isEmpty()?parsed.host():name.trim(),parsed.toString(),false);return PlaylistImportJobs.enqueue(context,id,"url","");});
    }
    private void error() { post(() -> host.status("No se pudo completar la operación. Comprueba la lista y el espacio disponible.")); }
}
