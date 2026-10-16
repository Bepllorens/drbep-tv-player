package com.drbep.tvplayer;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Explicit, short-lived LAN session. No filesystem browsing or playback credentials are exposed. */
final class DeviceWebPanel implements AutoCloseable {
    interface Host { default String metadataIndex(String command)throws Exception{throw new IOException();}  default String blackBox()throws Exception{return "{}";} default byte[] blackBoxExport()throws Exception{throw new IOException();} default String blackBoxCommand(String command)throws Exception{throw new IOException();} default byte[] screen()throws Exception{throw new IOException("Unavailable");} default String ui()throws Exception{throw new IOException("Unavailable");} default byte[] artwork(String id)throws Exception {throw new IOException("Unavailable");} String status(); long importPlaylist(InputStream input) throws Exception; default String enqueuePlaylist(InputStream input)throws Exception {return "{\"entries\":"+importPlaylist(input)+"}";} default String manage(String json) throws Exception { throw new IOException("Unavailable"); } }
    static final class PanelError extends IOException { PanelError(String message){super(message);} }
    private final Host host;
    private final byte[] page;
    private final Set<Socket> sockets=ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor workers=new ThreadPoolExecutor(2,2,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(4));
    private final AtomicInteger failures=new AtomicInteger();
    private final SecureRandom random=new SecureRandom();
    private volatile ServerSocket server;
    private volatile boolean closed;
    private String pin, token;
    private long expires;
    DeviceWebPanel(Host host,byte[] page){this.host=host;this.page=page.clone();}
    synchronized int start() throws IOException {
        if(server!=null)throw new IllegalStateException("Already started");
        pin=String.format(Locale.ROOT,"%06d",random.nextInt(1000000));
        byte[] secret=new byte[32];random.nextBytes(secret);token=Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        expires=System.nanoTime()+TimeUnit.MINUTES.toNanos(20);
        server=new ServerSocket(0); server.setSoTimeout(1000);
        Thread accept=new Thread(() -> {
            while(!closed){
                if(System.nanoTime()>=expires){close();break;}
                try {Socket socket=server.accept();socket.setSoTimeout(30000);sockets.add(socket);
                    try {workers.execute(() -> serve(socket));}catch(RejectedExecutionException e){sockets.remove(socket);try{socket.close();}catch(IOException ignored){}}
                } catch(SocketTimeoutException ignored){}catch(IOException e){if(!closed)close();}
            }
        },"drbep-web-panel");accept.setDaemon(true);accept.start();return server.getLocalPort();
    }
    String pin(){return pin;}
    boolean running(){return !closed&&server!=null;}
    private void serve(Socket socket){
        try(Socket owned=socket){
            InputStream in=new BufferedInputStream(socket.getInputStream(),16384);OutputStream out=socket.getOutputStream();
            String first=line(in,4096);String[] request=first.split(" ");
            if(request.length!=3){reply(out,400,"text/plain","Invalid request");return;}
            Map<String,String> headers=new HashMap<>();int total=first.length();
            while(true){String h=line(in,4096);total+=h.length();if(total>8192)throw new IOException("Headers too large");if(h.isEmpty())break;int colon=h.indexOf(':');if(colon<=0)throw new IOException("Invalid header");String key=h.substring(0,colon).trim().toLowerCase(Locale.ROOT);if(headers.put(key,h.substring(colon+1).trim())!=null)throw new IOException("Duplicate header");}
            String expected=socket.getLocalAddress().getHostAddress()+":"+server.getLocalPort();
            if(!expected.equals(headers.get("host"))){reply(out,403,"text/plain","Invalid host");return;}
            String origin=headers.get("origin");if(origin!=null&&!origin.equals("http://"+expected)){reply(out,403,"text/plain","Invalid origin");return;}
            if(headers.containsKey("transfer-encoding")){reply(out,400,"text/plain","Content-Length required");return;}
            long length=Long.parseLong(headers.getOrDefault("content-length","0"));
            if(length<0||length>256L*1024*1024){reply(out,413,"text/plain","Upload too large");return;}
            String path=request[1],method=request[0];
            if(method.equals("GET")&&path.equals("/")){reply(out,200,"text/html; charset=utf-8",page);return;}
            if(method.equals("POST")&&path.equals("/session")){
                if(length!=6){reply(out,401,"text/plain","Invalid PIN");return;}
                byte[] supplied=read(in,6);
                if(!MessageDigest.isEqual(supplied,pin.getBytes(StandardCharsets.US_ASCII))){reply(out,401,"text/plain","Invalid PIN");if(failures.incrementAndGet()>=5)close();return;}
                reply(out,200,"application/json","{\"token\":\""+token+"\"}");return;
            }
            String supplied=headers.getOrDefault("x-panel-token","");
            if(closed||System.nanoTime()>=expires||!MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8),token.getBytes(StandardCharsets.UTF_8))){reply(out,401,"text/plain","Session expired");return;}
            if(path.equals("/metadata-index")){
                try{
                    if(method.equals("GET"))reply(out,200,"application/json",host.metadataIndex(""));
                    else if(method.equals("POST")&&length>0&&length<=128){
                        String command=new org.json.JSONObject(new String(read(in,(int)length),StandardCharsets.UTF_8)).getString("command");
                        if(!Arrays.asList("start","pause","resume","retry").contains(command)){reply(out,400,"text/plain","Invalid command");return;}
                        reply(out,200,"application/json",host.metadataIndex(command));
                    }else reply(out,400,"text/plain","Invalid request");
                }catch(Exception e){reply(out,503,"text/plain","Indexación no disponible");}
                return;
            }
            if(path.equals("/blackbox")||path.equals("/blackbox-export")){
                try{
                    if(method.equals("GET")&&path.equals("/blackbox"))reply(out,200,"application/json",host.blackBox());
                    else if(method.equals("GET")&&path.equals("/blackbox-export"))reply(out,200,"application/zip",host.blackBoxExport());
                    else if(method.equals("POST")&&path.equals("/blackbox")&&length>0&&length<=128){
                        String command=new org.json.JSONObject(new String(read(in,(int)length),StandardCharsets.UTF_8)).getString("command");
                        reply(out,200,"application/json",host.blackBoxCommand(command));
                    }else reply(out,400,"text/plain","Invalid request");
                }catch(Exception e){reply(out,503,"text/plain","Diagnóstico no disponible");}
                return;
            }
            if(method.equals("GET")&&(path.equals("/screen")||path.equals("/ui"))){
                try{if(path.equals("/screen"))reply(out,200,"image/jpeg",host.screen());else reply(out,200,"application/json",host.ui());}
                catch(Exception e){reply(out,503,"text/plain","Pantalla no disponible");}return;
            }
            if(method.equals("GET")&&path.equals("/status")){reply(out,200,"application/json",host.status());return;}
            if(method.equals("POST")&&path.equals("/artwork")){
                if(length>1024){reply(out,413,"text/plain","Request too large");return;}
                try {String id=new org.json.JSONObject(new String(read(in,(int)length),StandardCharsets.UTF_8)).getString("id");reply(out,200,"image/jpeg",host.artwork(id));}
                catch(Exception e){reply(out,404,"text/plain","Artwork unavailable");}return;
            }
            if(method.equals("POST")&&path.equals("/manage")){
                if(length>65536){reply(out,413,"text/plain","Request too large");return;}
                try {reply(out,200,"application/json",host.manage(new String(read(in,(int)length),StandardCharsets.UTF_8)));}
                catch(Exception e){reply(out,400,"application/json",new org.json.JSONObject().put("error",e instanceof PanelError?e.getMessage():"No se pudo completar la operación. Comprueba los datos y vuelve a intentarlo.").toString());}
                return;
            }
            if(method.equals("GET")&&path.equals("/report")){reply(out,200,"application/json",host.status());return;}
            if(method.equals("POST")&&path.equals("/playlist")){
                if(length==0){reply(out,400,"text/plain","Empty playlist");return;}
                try {String queued=host.enqueuePlaylist(new Body(in,length));reply(out,200,"application/json",queued);}
                catch(Exception e){reply(out,400,"text/plain","Import failed. Previous lists were retained.");}
                return;
            }
            reply(out,404,"text/plain","Not found");
        }catch(Exception ignored){}finally{sockets.remove(socket);}
    }
    static final class Body extends FilterInputStream {
        private long remaining;
        Body(InputStream in,long length){super(in);remaining=length;}
        @Override public int read() throws IOException {byte[] one=new byte[1];int n=read(one,0,1);return n<0?-1:one[0]&255;}
        @Override public int read(byte[] b,int off,int len)throws IOException {
            if(len==0)return 0;if(remaining==0)return -1;if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
            int n=in.read(b,off,(int)Math.min(len,remaining));if(n<0)throw new EOFException("Truncated upload");remaining-=n;return n;
        }
    }
    private static byte[] read(InputStream in,int count)throws IOException {byte[] b=new byte[count];int offset=0;while(offset<count){int n=in.read(b,offset,count-offset);if(n<0)throw new EOFException();offset+=n;}return b;}
    private static String line(InputStream in,int limit)throws IOException {ByteArrayOutputStream b=new ByteArrayOutputStream();int c;while((c=in.read())!=-1){if(c=='\n')return b.toString("US-ASCII").replaceFirst("\\r$","");if(b.size()>=limit)throw new IOException("Line too long");b.write(c);}throw new EOFException();}
    private static void reply(OutputStream out,int code,String type,String text)throws IOException {reply(out,code,type,text.getBytes(StandardCharsets.UTF_8));}
    private static void reply(OutputStream out,int code,String type,byte[] body)throws IOException {
        out.write(("HTTP/1.1 "+code+" Response\r\nContent-Type: "+type+"\r\nContent-Length: "+body.length+"\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nContent-Security-Policy: default-src 'self'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; img-src 'self' blob:; frame-ancestors 'none'\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));out.write(body);out.flush();
    }
    @Override public synchronized void close(){if(closed)return;closed=true;try{if(server!=null)server.close();}catch(IOException ignored){}for(Socket s:sockets)try{s.close();}catch(IOException ignored){}workers.shutdownNow();}
}
