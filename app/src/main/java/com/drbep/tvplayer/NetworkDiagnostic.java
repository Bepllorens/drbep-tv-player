package com.drbep.tvplayer;

import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Explicit system-DNS probe. Never changes the playback client or sends its credentials. */
final class NetworkDiagnostic {
    interface Probe { JSONObject run(String base) throws Exception; }
    static final NetworkDiagnostic SHARED = new NetworkDiagnostic(NetworkDiagnostic::probe, 6000, 30000);
    private final Probe probe;
    private final long timeoutMs, cooldownMs;
    private final AtomicBoolean busy = new AtomicBoolean();
    private volatile long nextAllowed;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "drbep-network-check"); t.setDaemon(true); return t;
    });
    NetworkDiagnostic(Probe probe,long timeoutMs,long cooldownMs) {
        this.probe=probe;this.timeoutMs=timeoutMs;this.cooldownMs=cooldownMs;
    }
    String check(String base) throws Exception {
        if(!busy.compareAndSet(false,true))return state("busy");
        if(System.nanoTime()<nextAllowed){busy.set(false);return state("cooldown");}
        nextAllowed=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(cooldownMs);
        Future<JSONObject> result=worker.submit(()->{
            try{return probe.run(base);}
            finally{busy.set(false);}
        });
        try{return result.get(timeoutMs,TimeUnit.MILLISECONDS).toString();}
        catch(TimeoutException e){return state("timeout");}
        catch(ExecutionException e){return state("unavailable");}
        // Do not release the slot on timeout: a platform DNS lookup may ignore interruption.
        // At most one task survives; subsequent requests cannot create a queue of DNS calls.
    }
    private static String state(String value)throws Exception {
        return new JSONObject().put("state",value).put("target","Servidor DRBEP").toString();
    }
    static URL target(String base) throws Exception {
        URI uri=new URI(base);
        if(!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))throw new IllegalArgumentException();
        if(uri.getHost()==null || uri.getUserInfo()!=null)throw new IllegalArgumentException();
        return new URI(uri.getScheme(),null,uri.getHost(),uri.getPort(),"/health",null,null).toURL();
    }
    private static JSONObject probe(String base)throws Exception {
        URL url=target(base);
        JSONObject out=new JSONObject().put("target","Servidor DRBEP").put("state","complete");
        long start=System.nanoTime();
        try {
            InetAddress[] addresses=InetAddress.getAllByName(url.getHost());
            if(addresses.length==0)throw new UnknownHostException();
            out.put("dns_ms",elapsed(start)).put("dns","ok");
        }catch(UnknownHostException e){return out.put("dns","failed").put("dns_ms",elapsed(start));}
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();
        try {
            connection.setConnectTimeout(2000);connection.setReadTimeout(2000);
            connection.setInstanceFollowRedirects(false);connection.setUseCaches(false);
            connection.setRequestMethod("HEAD");
            start=System.nanoTime();
            int status=connection.getResponseCode();
            out.put("http_status",status).put("connection","ok");
        }catch(javax.net.ssl.SSLException e){out.put("connection","tls_error");}
        catch(java.net.SocketTimeoutException e){out.put("connection","timeout");}
        catch(java.io.IOException e){out.put("connection","failed");}
        finally{out.put("connection_ms",elapsed(start));connection.disconnect();}
        return out;
    }
    private static long elapsed(long start){return TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);}
}
