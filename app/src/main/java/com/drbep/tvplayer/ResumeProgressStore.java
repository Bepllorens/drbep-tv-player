package com.drbep.tvplayer;

import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** One serial writer with immutable snapshots; does not retain an Activity or player. */
final class ResumeProgressStore {
    static final class Entry {
        final long position, duration, updatedAt;
        Entry(long position, long duration, long updatedAt) {
            this.position=position; this.duration=duration; this.updatedAt=updatedAt;
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Entry)) return false;
            Entry e=(Entry)other;
            return position==e.position && duration==e.duration && updatedAt==e.updatedAt;
        }
        @Override public int hashCode(){return Objects.hash(position,duration,updatedAt);}
    }
    static final class Snapshot {
        final boolean recording;
        final Map<String,Entry> entries;
        private Snapshot(boolean recording, Map<String,Entry> entries) {
            this.recording=recording;
            this.entries=Collections.unmodifiableMap(entries);
        }
        static Snapshot capture(boolean recording, Map<String,Long> positions,
                                Map<String,Long> durations, Map<String,Long> times, long now) {
            Map<String,Entry> entries=new LinkedHashMap<>();
            for (Map.Entry<String,Long> row:positions.entrySet()) {
                String id=row.getKey();
                long pos=row.getValue()==null?0:Math.max(0,row.getValue());
                if(id==null || id.trim().isEmpty() || pos==0)continue;
                Long duration=durations.get(id), time=times.get(id);
                entries.put(id,new Entry(pos,duration==null?0:Math.max(0,duration),
                        time==null?now:Math.max(0,time)));
            }
            return new Snapshot(recording,entries);
        }
        String encode() throws Exception {
            JSONObject json=new JSONObject();
            for(Map.Entry<String,Entry> row:entries.entrySet()) {
                Entry e=row.getValue();
                json.put(row.getKey(),new JSONObject()
                        .put(recording?"position_ms":"position",e.position)
                        .put(recording?"duration_ms":"duration",e.duration)
                        .put("updated_at",e.updatedAt));
            }
            return json.toString();
        }
        void restore(Map<String,Long> positions, Map<String,Long> durations, Map<String,Long> times) {
            for(Map.Entry<String,Entry> row:entries.entrySet()) {
                positions.put(row.getKey(),row.getValue().position);
                durations.put(row.getKey(),row.getValue().duration);
                times.put(row.getKey(),row.getValue().updatedAt);
            }
        }
        @Override public boolean equals(Object other) {
            return other instanceof Snapshot && recording==((Snapshot)other).recording
                    && entries.equals(((Snapshot)other).entries);
        }
        @Override public int hashCode(){return Objects.hash(recording,entries);}
    }
    static final class Session {
        final long id;
        final String scope;
        Session(long id,String scope){this.id=id;this.scope=scope;}
    }
    interface Sink { boolean write(String key,Snapshot snapshot) throws Exception; }
    private static final class Write {
        final String key;
        final Snapshot snapshot;
        final long epoch;
        Write(String key,Snapshot snapshot,long epoch){this.key=key;this.snapshot=snapshot;this.epoch=epoch;}
    }
    private final Executor executor;
    private final Sink sink;
    private final LinkedHashMap<String,Write> pending=new LinkedHashMap<>();
    private final Map<String,Snapshot> latest=new HashMap<>(), saved=new HashMap<>();
    private Session session;
    private long epoch, sessionId;
    private boolean scheduled;
    private Write writing;
    ResumeProgressStore(Sink sink) {
        this(sink,Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"drbep-progress");t.setDaemon(true);return t;}));
    }
    ResumeProgressStore(Sink sink,Executor executor){this.sink=sink;this.executor=executor;}
    synchronized Session openSession(String scope) {
        scope=scope==null?"":scope;
        if(session==null || !session.scope.equals(scope)){
            epoch++;pending.clear();latest.clear();saved.clear();
        }
        session=new Session(++sessionId,scope);
        return session;
    }
    synchronized boolean current(Session owner){return owner!=null && session==owner;}
    synchronized Snapshot latest(Session owner,String key){return current(owner)?latest.get(key):null;}
    synchronized void retry(Session owner,String key) {
        Snapshot value=latest(owner,key);
        if(value!=null)submit(owner,key,value);
    }
    synchronized boolean submit(Session owner,String key,Snapshot snapshot) {
        if(!current(owner))return false;
        Snapshot old=latest.get(key);
        if(snapshot.equals(old) && (pending.containsKey(key)
                || (writing!=null && writing.epoch==epoch && writing.key.equals(key)
                    && writing.snapshot.equals(snapshot)) || snapshot.equals(saved.get(key))))return false;
        latest.put(key,snapshot);
        pending.put(key,new Write(key,snapshot,epoch));
        if(!scheduled){
            scheduled=true;
            try{executor.execute(this::drain);}
            catch(RuntimeException rejected){scheduled=false;throw rejected;}
        }
        return true;
    }
    private void drain() {
        for(;;) {
            Write next;
            synchronized(this) {
                if(pending.isEmpty()){scheduled=false;writing=null;return;}
                String key=pending.keySet().iterator().next();
                next=pending.remove(key);writing=next;
            }
            boolean ok=false;
            try{ok=sink.write(next.key,next.snapshot);}catch(Exception ignored){}
            synchronized(this){
                if(ok && next.epoch==epoch)saved.put(next.key,next.snapshot);
                if(writing==next)writing=null;
            }
        }
    }
}
