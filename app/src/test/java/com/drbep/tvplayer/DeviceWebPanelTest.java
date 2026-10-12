package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;
public class DeviceWebPanelTest {
    private static String request(int port,String method,String path,String extra,String body,int declared) throws Exception {
        try(Socket s=new Socket("127.0.0.1",port)) {
            s.setSoTimeout(3000);
            OutputStream out=s.getOutputStream();
            String headers=method+" "+path+" HTTP/1.1\r\nHost: 127.0.0.1:"+port+"\r\n"+extra+"Content-Length: "+declared+"\r\n\r\n";
            out.write(headers.getBytes(StandardCharsets.US_ASCII));out.write(body.getBytes(StandardCharsets.UTF_8));out.flush();s.shutdownOutput();
            ByteArrayOutputStream response=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=s.getInputStream().read(b))!=-1)response.write(b,0,n);return response.toString("UTF-8");
        }
    }
    @Test public void sessionUploadAndTruncationAreBounded() throws Exception {
        AtomicInteger complete=new AtomicInteger();
        DeviceWebPanel.Host host=new DeviceWebPanel.Host(){public String status(){return "{\"state\":\"ready\"}";}public long importPlaylist(InputStream in)throws Exception{int n=0;byte[] b=new byte[1024];int read;while((read=in.read(b))!=-1)n+=read;complete.incrementAndGet();return n;}};
        try(DeviceWebPanel panel=new DeviceWebPanel(host,"<html>panel</html>".getBytes(StandardCharsets.UTF_8))){
            int port=panel.start();
            assertTrue(request(port,"GET","/status","","",0).startsWith("HTTP/1.1 401"));
            assertTrue(request(port,"POST","/session","Origin: http://other.test\r\n",panel.pin(),6).startsWith("HTTP/1.1 403"));
            String login=request(port,"POST","/session","",panel.pin(),6);
            String token=new JSONObject(login.split("\r\n\r\n",2)[1]).getString("token");
            String auth="X-Panel-Token: "+token+"\r\n";
            assertTrue(request(port,"GET","/status",auth,"",0).contains("\"state\":\"ready\""));
            assertTrue(request(port,"POST","/playlist",auth,"abcdef",6).contains("\"entries\":6"));
            assertTrue(request(port,"POST","/playlist",auth,"abc",6).startsWith("HTTP/1.1 400"));
            assertEquals(1,complete.get());
            assertTrue(request(port,"POST","/playlist",auth,"",256*1024*1024+1).startsWith("HTTP/1.1 413"));
        }
    }
    @Test public void rejectsRebindingAndChunkedRequests() throws Exception {
        try(DeviceWebPanel panel=new DeviceWebPanel(new DeviceWebPanel.Host(){public String status(){return "{}";}public long importPlaylist(InputStream in){return 0;}},new byte[0])) {
            int port=panel.start();
            assertTrue(request(port,"POST","/playlist","Transfer-Encoding: chunked\r\n","",0).startsWith("HTTP/1.1 400"));
            try(Socket socket=new Socket("127.0.0.1",port)) {
                socket.setSoTimeout(2000);
                socket.getOutputStream().write("GET / HTTP/1.1\r\nHost: attacker.example\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                assertTrue(new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII)).readLine().startsWith("HTTP/1.1 403"));
            }
        }
    }
    @Test public void locksAfterFiveWrongPins()throws Exception {
        try(DeviceWebPanel panel=new DeviceWebPanel(new DeviceWebPanel.Host(){public String status(){return "{}";}public long importPlaylist(InputStream in){return 0;}},new byte[0])) {
            int port=panel.start();String bad=panel.pin().equals("000000")?"111111":"000000";
            for(int i=0;i<5;i++)assertTrue(request(port,"POST","/session","",bad,6).startsWith("HTTP/1.1 401"));
            for(int i=0;i<20 && panel.running();i++)Thread.sleep(10);
            assertFalse(panel.running());
        }
    }
    @Test public void screenAndUiRequireSession()throws Exception {
        AtomicInteger captures=new AtomicInteger();
        try(DeviceWebPanel panel=new DeviceWebPanel(new DeviceWebPanel.Host(){public String status(){return "{}";}public long importPlaylist(InputStream in){return 0;}public byte[] screen(){captures.incrementAndGet();return new byte[]{1,2,3};}public String ui(){return "{\"nodes\":[]}";}},new byte[0])){
            int port=panel.start();
            for(String route:new String[]{"/screen","/ui"})assertTrue(request(port,"GET",route,"","",0).startsWith("HTTP/1.1 401"));assertEquals(0,captures.get());
            String login=request(port,"POST","/session","",panel.pin(),6);String token=new JSONObject(login.split("\r\n\r\n",2)[1]).getString("token");String auth="X-Panel-Token: "+token+"\r\n";
            assertTrue(request(port,"GET","/screen",auth,"",0).startsWith("HTTP/1.1 200"));assertEquals(1,captures.get());assertTrue(request(port,"GET","/ui",auth,"",0).contains("nodes"));
        }
    }
}
