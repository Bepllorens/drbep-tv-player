package com.drbep.tvplayer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.*;
import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Process singleton. Only application context retained; no Activity or content identifiers. */
final class BlackBox {
    private static volatile BlackBoxStore store;
    private static SharedPreferences prefs;
    private static volatile boolean foreground;
    private static final AtomicLong heartbeat=new AtomicLong();
    private static long posted,lastSample;
    private static final AtomicLong operations=new AtomicLong();
    static long operation(){return operations.incrementAndGet();}
    static long elapsed(long start){return Math.max(0,(System.nanoTime()-start)/1000000);}
    static synchronized void init(Context context){
        if(store!=null)return;
        Context app=context.getApplicationContext();
        prefs=app.getSharedPreferences("blackbox",0);
        store=new BlackBoxStore(new File(app.getNoBackupFilesDir(),"blackbox"),BuildConfig.VERSION_CODE);
        store.setEnabled(prefs.getBoolean("enabled",true));
        Handler main=new Handler(Looper.getMainLooper());
        ScheduledExecutorService monitor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"drbep-health");t.setDaemon(true);return t;});
        monitor.scheduleWithFixedDelay(()->{
            try{
                if(!foreground||!store.enabled()){posted=0;return;}
                long now=SystemClock.uptimeMillis();
                if(posted!=0&&heartbeat.get()<posted){
                    if(now-posted>=2000)event(BlackBoxStore.Event.UI_STALL,0,now-posted,0,0);
                }else{
                    posted=now;long sent=now;main.post(()->heartbeat.set(sent));
                }
                if(now-lastSample>=(store.detailed()?5000:30000)){
                    lastSample=now;Runtime r=Runtime.getRuntime();
                    event(BlackBoxStore.Event.MEMORY,0,r.totalMemory()-r.freeMemory(),r.maxMemory(),0);
                }
            }catch(Exception ignored){}
        },2,2,TimeUnit.SECONDS);
        Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            try{event(BlackBoxStore.Event.CRASH,0,error instanceof OutOfMemoryError?1:error instanceof RuntimeException?2:3,0,0);store.flushCrash();}catch(Throwable ignored){}
            if(previous!=null)previous.uncaughtException(thread,error);
            else {android.os.Process.killProcess(android.os.Process.myPid());System.exit(10);}
        });
        monitor.execute(()->{
            if(Build.VERSION.SDK_INT>=30)try{
                ActivityManager am=(ActivityManager)app.getSystemService(Context.ACTIVITY_SERVICE);
                long last=prefs.getLong("exitAt",0),latest=last;
                for(android.app.ApplicationExitInfo info:am.getHistoricalProcessExitReasons(null,0,5)){
                    if(info.getTimestamp()>last){event(BlackBoxStore.Event.PROCESS_EXIT,0,info.getReason(),info.getImportance(),info.getTimestamp());latest=Math.max(latest,info.getTimestamp());}
                }
                prefs.edit().putLong("exitAt",latest).apply();
            }catch(Exception ignored){}
        });
    }
    static void event(BlackBoxStore.Event e,long op,long a,long b,long c){BlackBoxStore s=store;if(s!=null)s.event(e,op,a,b,c);}
    static void foreground(boolean value){foreground=value;event(value?BlackBoxStore.Event.FOREGROUND:BlackBoxStore.Event.BACKGROUND,0,0,0,0);}
    static HttpRequestTrace httpRequest() {
        BlackBoxStore active=store;
        if(active==null || !active.detailed())return HttpRequestTrace.OFF;
        return new HttpRequestTrace(operation(),()->System.nanoTime()/1000000,
                (phase,op,a,b,c)->event(phase==HttpRequestTrace.START?BlackBoxStore.Event.HTTP_REQUEST_START:
                        phase==HttpRequestTrace.HEADERS?BlackBoxStore.Event.HTTP_RESPONSE_HEADERS:
                        BlackBoxStore.Event.HTTP_REQUEST_END,op,a,b,c));
    }
    static void guideBind(boolean cached,int rows,long totalUs,long prepareUs,long formatUs,long selectionUs,long bindUs) {
        BlackBoxStore active=store;
        if(active==null || !active.detailed())return;
        long op=operation();
        active.event(cached?BlackBoxStore.Event.GUIDE_BIND_CACHED:BlackBoxStore.Event.GUIDE_BIND_COLD,op,totalUs,formatUs,rows);
        active.event(BlackBoxStore.Event.GUIDE_BIND_PHASES,op,prepareUs,selectionUs,bindUs);
    }
    static long sampleInterval(){BlackBoxStore s=store;return s!=null&&s.detailed()?5000:30000;}
    static String detailLease(String client,long sequence,boolean active)throws Exception{
        return store.detailLease(client,sequence,active);
    }
    static String summary()throws Exception{return store.summary();}
    static byte[] export()throws Exception{return store.export();}
    static byte[] export(int minutes)throws Exception {
        if(minutes!=5 && minutes!=15 && minutes!=60)throw new IllegalArgumentException();
        long to=System.currentTimeMillis();return store.export(Math.max(0,to-minutes*60000L),to);
    }
    static String command(String command)throws Exception{
        switch(command){
            case "mark":return store.mark();
            case "detail":store.detail();break;
            case "enable":case "disable":boolean enabled=command.equals("enable");store.setEnabled(enabled);prefs.edit().putBoolean("enabled",enabled).apply();break;
            default:throw new IllegalArgumentException();
        }return "{\"ok\":true}";
    }
}
