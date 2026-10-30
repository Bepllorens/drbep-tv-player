package com.drbep.tvplayer;

import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.zip.*;

/** Bounded, numeric-only telemetry. Producers never perform disk I/O. */
final class BlackBoxStore implements AutoCloseable {
    enum Event { GUIDE_BIND_COLD, GUIDE_BIND_CACHED, GUIDE_BIND_PHASES, SAFETY_STOP_BEGIN, SAFETY_PROGRESS_SAVED, SAFETY_STOP_ISSUED, SAFETY_STOP_ACK, PLAY_RESOLVE_BEGIN, PLAY_RESOLVE_LINK, HTTP_REQUEST_START, HTTP_RESPONSE_HEADERS, HTTP_REQUEST_END, SESSION, ACTIVITY, FOREGROUND, BACKGROUND, WINDOW, PLAYER_READY, STARTUP_FRAME,
        CATALOG_READY, CATALOG_APPLY, HOME_MODEL, NAV_HOME, NAV_SECTION, NAV_KEY, PLAY_PREPARE, PLAY_STATE,
        PLAY_FRAME, PLAY_MANIFEST_LOADED, PLAY_MEDIA_LOADED, PLAY_VIDEO_DECODER, SURFACE_FRAME, PLAY_ERROR, VIDEO_COUNTERS, AUDIO_COUNTERS, PLAYBACK_SAMPLE, MEMORY, MEMORY_PRESSURE, UI_STALL, CRASH,
        PROCESS_EXIT, MARK, DETAIL, ENABLED, EPG_LOAD, GUIDE_LOAD, GUIDE_CACHE, GUIDE_HTTP, GUIDE_PARSE, GUIDE_FALLBACK, CATCHUP_PAGE, CATCHUP_STRIP, CATCHUP_DB_OPEN, CATCHUP_DB_COUNT, CATCHUP_DB_FETCH, CATCHUP_DB_DECODE, MULTI_OPEN, MULTI_CLOSE, MULTI_PREPARE, MULTI_STATE, MULTI_FRAME, MULTI_ERROR, MULTI_COUNTERS, ZAP_PRESS, ZAP_TUNE, ZAP_FRAME, ARTWORK_QUEUE_REJECT, PROGRESS_WRITE, DETAIL_LEASE }
    private static final class Entry {
        final Event type; final long wall,elapsed,op,a,b,c;
        Entry(Event t,long o,long x,long y,long z) {type=t;op=o;a=x;b=y;c=z;wall=System.currentTimeMillis();elapsed=System.nanoTime()/1000000;}
    }
    private final File dir;
    private final long segmentLimit;
    private final String session=UUID.randomUUID().toString();
    private final ArrayBlockingQueue<Entry> queue=new ArrayBlockingQueue<>(512);
    private final ScheduledExecutorService writer=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"drbep-blackbox");t.setDaemon(true);return t;});
    private final AtomicLong lost=new AtomicLong(), failures=new AtomicLong();
    private volatile boolean enabled=true,closed;
    private volatile long detailedUntil;
    private final DiagnosticDetailLease detailObservers=new DiagnosticDetailLease();
    private final int version;
    private long markId;
    BlackBoxStore(File dir,int version) {this(dir,version,512*1024);}
    BlackBoxStore(File dir,int version,long segmentLimit) {
        this.dir=dir;this.version=version;this.segmentLimit=segmentLimit;
        writer.scheduleWithFixedDelay(this::drain,1,1,TimeUnit.SECONDS);
        event(Event.SESSION,0,version,0,0);
    }
    boolean enabled(){return enabled;}
    boolean detailed(){return enabled && (System.nanoTime()/1000000<detailedUntil || detailObservers.activeCount()>0);}
    String detailLease(String client,long sequence,boolean active) throws Exception {
        int before=detailObservers.activeCount();
        if(!detailObservers.update(client,sequence,active))throw new IllegalStateException("Too many observers");
        int count=detailObservers.activeCount();
        if(before!=count)event(Event.DETAIL_LEASE,0,count,DiagnosticDetailLease.LEASE_MS,0);
        return new JSONObject().put("ok",true).put("leaseMs",DiagnosticDetailLease.LEASE_MS).put("detailed",detailed()).toString();
    }
    void setEnabled(boolean value){enabled=value;if(!value){queue.clear();detailObservers.clear();detailedUntil=0;}if(value)event(Event.ENABLED,0,1,0,0);}
    void detail(){detailedUntil=System.nanoTime()/1000000+600000;event(Event.DETAIL,0,600000,0,0);}
    void event(Event type,long op,long a,long b,long c){
        if(enabled&&!closed&&!queue.offer(new Entry(type,op,a,b,c)))lost.incrementAndGet();
    }
    private void drain(){
        try {
            if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException();
            ArrayList<Entry> batch=new ArrayList<>(512);queue.drainTo(batch,512);
            OutputStream output=null;
            try { for(Entry e:batch){
                JSONObject j=new JSONObject().put("session",session).put("version",version).put("at",e.wall)
                    .put("elapsed",e.elapsed).put("event",e.type.name()).put("op",e.op).put("a",e.a).put("b",e.b).put("c",e.c);
                byte[] bytes=(j.toString()+"\n").getBytes(StandardCharsets.UTF_8);
                File active=new File(dir,"events-0.jsonl");
                if(active.length()+bytes.length>segmentLimit){
                    if(output!=null){output.close();output=null;}rotate();
                }
                if(output==null)output=new FileOutputStream(active,true);
                output.write(bytes);
            }} finally {if(output!=null)output.close();}
        }catch(Exception e){failures.incrementAndGet();}
    }
    private void rotate()throws IOException{
        File last=new File(dir,"events-3.jsonl");if(last.exists()&&!last.delete())throw new IOException();
        for(int i=2;i>=0;i--){File f=new File(dir,"events-"+i+".jsonl");if(f.exists()&&!f.renameTo(new File(dir,"events-"+(i+1)+".jsonl")))throw new IOException();}
    }
    private List<File> logs(){
        List<File> out=new ArrayList<>();for(int i=3;i>=0;i--){File f=new File(dir,"events-"+i+".jsonl");if(f.isFile())out.add(f);}return out;
    }
    private <T>T call(Callable<T> task)throws Exception{
        Future<T> f=writer.submit(task);
        try{return f.get(5,TimeUnit.SECONDS);}catch(Exception e){f.cancel(false);throw e;}
    }
    String summary()throws Exception{return call(()->{
        drain();ArrayDeque<JSONObject> recent=new ArrayDeque<>();StartupTraceSummary startup=new StartupTraceSummary();PlaybackTraceSummary playback=new PlaybackTraceSummary();
        Map<String,List<Long>> values=new TreeMap<>();Map<String,Long> counts=new TreeMap<>();
        for(File f:logs())try(BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(f),StandardCharsets.UTF_8))){
            String line;while((line=in.readLine())!=null){try{
                JSONObject j=new JSONObject(line);startup.accept(j);playback.accept(j);String k=j.getString("event");
                if(k.equals("PLAY_FRAME")&&j.optLong("c")==1)continue;
                counts.put(k,counts.getOrDefault(k,0L)+1);
                List<Long> v=values.computeIfAbsent(k,ignored->new ArrayList<>());if(v.size()==256)v.remove(0);v.add(j.getLong("a"));
                recent.add(j);if(recent.size()>200)recent.removeFirst();
            }catch(JSONException ignored){}}
        }
        JSONArray stats=new JSONArray();for(String k:counts.keySet()){
            List<Long> v=values.get(k);Collections.sort(v);
            stats.put(new JSONObject().put("event",k).put("count",counts.get(k)).put("p50",v.get(v.size()/2))
                .put("p95",v.get(Math.min(v.size()-1,(int)Math.ceil(v.size()*.95)-1))).put("max",v.get(v.size()-1)));
        }
        return new JSONObject().put("enabled",enabled).put("detailed",detailed()).put("session",session)
            .put("lost",lost.get()).put("writeErrors",failures.get()).put("bytes",size()).put("limitBytes",segmentLimit*12)
            .put("detailObservers",detailObservers.activeCount()).put("playbacks",playback.json()).put("startups",startup.json()).put("stats",stats).put("events",new JSONArray(recent)).toString();
    });}
    private long size(){long n=0;File[] files=dir.listFiles();if(files!=null)for(File f:files)if(f.isFile())n+=f.length();return n;}
    String mark()throws Exception{return call(()->{
        long id=++markId;event(Event.MARK,id,0,0,0);drain();snapshot();
        writer.schedule(()->{if(markId==id){drain();try{snapshot();}catch(IOException e){failures.incrementAndGet();}}},30,TimeUnit.SECONDS);
        return "{\"marked\":true,\"afterSeconds\":30}";
    });}
    private void snapshot()throws IOException{
        File target=new File(dir,"incident-"+(markId%2)+".jsonl");
        try(OutputStream out=new FileOutputStream(target)){
            byte[] b=new byte[8192];for(File f:logs())try(InputStream in=new FileInputStream(f)){int n;while((n=in.read(b))>0)out.write(b,0,n);}
        }
    }
    byte[] export()throws Exception{return export(0,Long.MAX_VALUE);}
    byte[] export(long from,long to)throws Exception{
        if(from<0 || to<from)throw new IllegalArgumentException("Invalid interval");
        final boolean filtered=from!=0 || to!=Long.MAX_VALUE;
        return call(()->{
        drain();ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(ZipOutputStream zip=new ZipOutputStream(bytes)){
            List<File> files=new ArrayList<>(logs());for(int i=0;i<2;i++){File f=new File(dir,"incident-"+i+".jsonl");if(f.isFile())files.add(f);}
            byte[] b=new byte[8192];for(File f:files){
                zip.putNextEntry(new ZipEntry(f.getName()));
                if(filtered) {
                    try(BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(f),StandardCharsets.UTF_8))){
                        String line;while((line=reader.readLine())!=null){
                            try {long at=new JSONObject(line).getLong("at");
                                if(at>=from && at<=to){zip.write(line.getBytes(StandardCharsets.UTF_8));zip.write(10);}
                            }catch(JSONException malformed){/* Ignore incomplete records only in filtered exports. */}
                        }
                    }
                } else try(InputStream in=new FileInputStream(f)){int n;while((n=in.read(b))>0)zip.write(b,0,n);}
                zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry("README.txt"));zip.write(("DRBEP black box schema 1. Numeric-only; no URLs, titles or credentials.\n"+
                (filtered ? "Filtered interval, inclusive UTC epoch ms: "+from+" to "+to+". Empty files mean no retained events in this interval.\n" : "Complete retained history.\n")+
                "Intervals may omit the start/end of an operation. Incident files may duplicate ring events.\n"+
                "at=UTC epoch ms; elapsed=monotonic ms; session=process; op=playback attempt.\n"+
                "PLAYBACK_SAMPLE a=position ms,b=buffer ms,c=flags (state low 3 bits,requested 8,playing 16,loading 32,suppressed 64). AUDIO_COUNTERS a=rendered,b=skipped,c=buffer ms; not proof of audible output.\n"+
                "Timing events: a=ms. VIDEO_COUNTERS a=rendered,b=dropped,c=buffer ms. MEMORY a=used heap bytes,b=max heap bytes.\n"+
                "PLAY_MANIFEST_LOADED/PLAY_MEDIA_LOADED a=load duration ms,b=bytes,c=Media3 track type; first completed nonempty load before first frame, not DNS/TCP or sum of playlists. PLAY_VIDEO_DECODER a=initialization duration ms.\n"+
                "PLAY_FRAME b=1 preview,c=1 repeated. PLAY_STATE a=Media3 state. PLAY_ERROR a=Media3 code.\n"+
                "NAV_KEY a=Android keycode. MEMORY_PRESSURE a=Android level. PROCESS_EXIT a=Android exit reason,b=importance.\n"+
                "CRASH a=1 OOM,2 runtime,3 other; no exception text. MARK=incident marker.\n"+
                "Ring 4 x 512 KiB plus two incident snapshots. Summary percentiles: last 256 samples per event, not whole session.\n").getBytes(StandardCharsets.UTF_8));zip.closeEntry();
        }return bytes.toByteArray();
    });}
    void flushCrash(){try{writer.submit(this::drain).get(250,TimeUnit.MILLISECONDS);}catch(Exception ignored){}}
    @Override public void close(){closed=true;writer.shutdownNow();try{writer.awaitTermination(2,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}drain();}
}
