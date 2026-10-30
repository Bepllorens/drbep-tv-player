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
    @Test public void imageCacheRequiresSessionPostAndEmptyBody() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        DeviceWebPanel.Host host=new DeviceWebPanel.Host(){
            public String status(){return "{}";}public long importPlaylist(InputStream in){return 0;}
            public String imageCache(){calls.incrementAndGet();return "{}";}
        };
        try(DeviceWebPanel panel=new DeviceWebPanel(host,new byte[0])){
            int port=panel.start();
            assertTrue(request(port,"POST","/image-cache","","",0).startsWith("HTTP/1.1 401"));
            String login=request(port,"POST","/session","",panel.pin(),6);
            String token=new JSONObject(login.split("\r\n\r\n",2)[1]).getString("token");
            String auth="X-Panel-Token: "+token+"\r\n";
            assertTrue(request(port,"GET","/image-cache",auth,"",0).startsWith("HTTP/1.1 400"));
            assertTrue(request(port,"POST","/image-cache",auth,"x",1).startsWith("HTTP/1.1 400"));
            assertTrue(request(port,"POST","/image-cache",auth+"Origin: http://other.test\r\n","",0).startsWith("HTTP/1.1 403"));
            assertTrue(request(port,"POST","/image-cache",auth,"",0).startsWith("HTTP/1.1 200"));
            assertEquals(1,calls.get());
        }
    }
    @Test public void networkProbeRequiresSessionPostAndEmptyBody() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        DeviceWebPanel.Host host=new DeviceWebPanel.Host(){
            public String status(){return "{}";}public long importPlaylist(InputStream in){return 0;}
            public String networkCheck(){calls.incrementAndGet();return "{}";}
        };
        try(DeviceWebPanel panel=new DeviceWebPanel(host,new byte[0])){
            int port=panel.start();
            assertTrue(request(port,"POST","/network-check","","",0).startsWith("HTTP/1.1 401"));
            String login=request(port,"POST","/session","",panel.pin(),6);
            String token=new JSONObject(login.split("\r\n\r\n",2)[1]).getString("token");
            String auth="X-Panel-Token: "+token+"\r\n";
            assertTrue(request(port,"GET","/network-check",auth,"",0).startsWith("HTTP/1.1 400"));
            assertTrue(request(port,"POST","/network-check",auth,"x",1).startsWith("HTTP/1.1 400"));
            assertTrue(request(port,"POST","/network-check",auth,"",0).startsWith("HTTP/1.1 200"));
            assertEquals(1,calls.get());
        }
    }
    @Test public void diagnosticObserverRequiresSessionAndValidBoundedBody() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        DeviceWebPanel.Host host=new DeviceWebPanel.Host(){
            public String status(){return "{}";}
            public long importPlaylist(InputStream input){return 0;}
            public String blackBoxLease(String client,long sequence,boolean active){
                calls.incrementAndGet();return "{\"ok\":true}";
            }
        };
        try(DeviceWebPanel panel=new DeviceWebPanel(host,new byte[0])){
            int port=panel.start();
            String body=new JSONObject().put("client","tab-test").put("sequence",1).put("active",true).toString();
            assertTrue(request(port,"POST","/blackbox-detail","",body,body.length()).startsWith("HTTP/1.1 401"));
            String login=request(port,"POST","/session","",panel.pin(),6);
            String token=new JSONObject(login.split("\r\n\r\n",2)[1]).getString("token");
            String auth="X-Panel-Token: "+token+"\r\n";
            assertTrue(request(port,"POST","/blackbox-detail",auth,body,body.length()).startsWith("HTTP/1.1 200"));
            assertTrue(request(port,"POST","/blackbox-detail",auth+"Origin: http://other.test\r\n",body,body.length()).startsWith("HTTP/1.1 403"));
            for(String invalid:new String[]{
                new JSONObject().put("client","bad/client").put("sequence",2).put("active",true).toString(),
                new JSONObject().put("client","tab").put("sequence",0).put("active",true).toString(),
                new JSONObject().put("client","tab").put("sequence",2).put("active","true").toString()}) {
                assertTrue(request(port,"POST","/blackbox-detail",auth,invalid,invalid.length()).startsWith("HTTP/1.1 400"));
            }
            assertTrue(request(port,"POST","/blackbox-detail",auth,"",257).startsWith("HTTP/1.1 400"));
            assertEquals(1,calls.get());
        }
    }
    @Test public void metadataControlsRequireSessionAndAllowOnlyKnownCommands() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        DeviceWebPanel.Host host=new DeviceWebPanel.Host(){
            public String status(){return "{}";}
            public long importPlaylist(InputStream in){return 0;}
            public String metadataIndex(String command){calls.incrementAndGet();return "{\"state\":\"paused\"}";}
        };
        try(DeviceWebPanel panel=new DeviceWebPanel(host,new byte[0])){
            int port=panel.start();
            assertTrue(request(port,"GET","/metadata-index","","",0).startsWith("HTTP/1.1 401"));
            assertEquals(0,calls.get());
            String login=request(port,"POST","/session","",panel.pin(),6);
            String token=new JSONObject(login.split("\r\n\r\n",2)[1]).getString("token");
            String auth="X-Panel-Token: "+token+"\r\n";
            assertTrue(request(port,"GET","/metadata-index",auth,"",0).contains("paused"));
            for(String command:new String[]{"start","pause","resume","retry"}){
                String body=new JSONObject().put("command",command).toString();
                assertTrue(request(port,"POST","/metadata-index",auth,body,body.length()).startsWith("HTTP/1.1 200"));
            }
            String invalid="{\"command\":\"delete\"}";
            assertTrue(request(port,"POST","/metadata-index",auth,invalid,invalid.length()).startsWith("HTTP/1.1 400"));
            assertEquals(5,calls.get());
            assertTrue(request(port,"POST","/metadata-index",auth,"",2049).startsWith("HTTP/1.1 400"));
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

    @Test public void blackBoxRequiresSessionAndChecksOrigin()throws Exception {
        AtomicInteger calls=new AtomicInteger();
        DeviceWebPanel.Host host=new DeviceWebPanel.Host(){
            public String status(){return "{}";}
            public long importPlaylist(InputStream in){return 0;}
            public String blackBox(){calls.incrementAndGet();return "{\"enabled\":true}";}
            public byte[] blackBoxExport(){calls.incrementAndGet();return new byte[]{1,2};}
            public String blackBoxCommand(String command){calls.incrementAndGet();return "{}";}
        };
        try(DeviceWebPanel panel=new DeviceWebPanel(host,new byte[0])){
            int port=panel.start();
            assertTrue(request(port,"GET","/blackbox","","",0).startsWith("HTTP/1.1 401"));
            assertTrue(request(port,"GET","/blackbox-export","","",0).startsWith("HTTP/1.1 401"));
            assertEquals(0,calls.get());
            String login=request(port,"POST","/session","",panel.pin(),6);
            String token=new JSONObject(login.split("\r\n\r\n",2)[1]).getString("token");
            String auth="X-Panel-Token: "+token+"\r\n";
            assertTrue(request(port,"GET","/blackbox",auth,"",0).contains("enabled"));
            assertTrue(request(port,"GET","/blackbox-export",auth,"",0).startsWith("HTTP/1.1 200"));
            String body="{\"command\":\"mark\"}";
            assertTrue(request(port,"POST","/blackbox",auth+"Origin: http://other.test\r\n",body,body.length()).startsWith("HTTP/1.1 403"));
            assertEquals(2,calls.get());
        }
    }

 @Test public void intervalExportRequiresAuthenticationAndValidPreset()throws Exception {
  AtomicInteger selected=new AtomicInteger();
  DeviceWebPanel.Host host=new DeviceWebPanel.Host(){
   public String status(){return "{}";}
   public long importPlaylist(InputStream in){return 0;}
   public byte[] blackBoxExport(int minutes){selected.set(minutes);return new byte[]{1};}
  };
  try(DeviceWebPanel panel=new DeviceWebPanel(host,new byte[0])){
   int port=panel.start();String body="{\"minutes\":5}";
   assertTrue(request(port,"POST","/blackbox-export","",body,body.length()).startsWith("HTTP/1.1 401"));
   assertEquals(0,selected.get());
   String login=request(port,"POST","/session","",panel.pin(),6);
   String auth="X-Panel-Token: "+new JSONObject(login.split("\r\n\r\n",2)[1]).getString("token")+"\r\n";
   for(int minutes:new int[]{5,15,60}){
    body="{\"minutes\":"+minutes+"}";
    assertTrue(request(port,"POST","/blackbox-export",auth,body,body.length()).startsWith("HTTP/1.1 200"));
    assertEquals(minutes,selected.get());
   }
   for(String bad:new String[]{"{}","bad","{\"minutes\":-1}","{\"minutes\":0}","{\"minutes\":61}"}){
    assertTrue(request(port,"POST","/blackbox-export",auth,bad,bad.length()).startsWith("HTTP/1.1 400"));
   }
   assertEquals(60,selected.get());
   assertTrue(request(port,"POST","/blackbox-export",auth,"",65).startsWith("HTTP/1.1 400"));
   assertTrue(request(port,"POST","/blackbox-export",auth+"Origin: http://other.test\r\n",body,body.length()).startsWith("HTTP/1.1 403"));
  }
 }

 @Test public void metadataSelectionIsForwardedOnlyAfterAuthentication()throws Exception {
  java.util.List<String> calls=new java.util.ArrayList<>();
  DeviceWebPanel.Host host=new DeviceWebPanel.Host(){
   public String status(){return "{}";}public long importPlaylist(InputStream in){return 0;}
   public String metadataIndexSelection(String command,String source,String kind){calls.add(command+":"+source+":"+kind);return "{}";}
  };
  try(DeviceWebPanel panel=new DeviceWebPanel(host,new byte[0])){
   int port=panel.start();String body=new JSONObject().put("command","start").put("source","list-a").put("kind","series").toString();
   assertTrue(request(port,"POST","/metadata-index","",body,body.length()).startsWith("HTTP/1.1 401"));
   String login=request(port,"POST","/session","",panel.pin(),6);
   String auth="X-Panel-Token: "+new JSONObject(login.split("\r\n\r\n",2)[1]).getString("token")+"\r\n";
   assertTrue(request(port,"POST","/metadata-index",auth,body,body.length()).startsWith("HTTP/1.1 200"));
   assertEquals(java.util.Arrays.asList("start:list-a:series"),calls);
   body=new JSONObject().put("command","start").put("kind","live").toString();
   assertTrue(request(port,"POST","/metadata-index",auth,body,body.length()).startsWith("HTTP/1.1 400"));
   assertEquals(1,calls.size());
  }
 }

 @Test public void metadataGroupDiscoveryAndStartKeepTheSelectedGroup()throws Exception {
  java.util.List<String> calls=new java.util.ArrayList<>();
  DeviceWebPanel.Host host=new DeviceWebPanel.Host(){
   public String status(){return "{}";}public long importPlaylist(InputStream in){return 0;}
   public String metadataIndexGroups(String source,String kind){calls.add("groups:"+source+":"+kind);return "{\"groups\":[\"Drama\"]}";}
   public String metadataIndexSelection(String command,String source,String kind,String group){calls.add(command+":"+source+":"+kind+":"+group);return "{}";}
  };
  try(DeviceWebPanel panel=new DeviceWebPanel(host,new byte[0])){
   int port=panel.start();String login=request(port,"POST","/session","",panel.pin(),6);
   String auth="X-Panel-Token: "+new JSONObject(login.split("\r\n\r\n",2)[1]).getString("token")+"\r\n";
   for(String command:new String[]{"groups","start"}){
    String body=new JSONObject().put("command",command).put("source","a").put("kind","series").put("group",command.equals("start")?"Drama":"").toString();
    assertTrue(request(port,"POST","/metadata-index",auth,body,body.length()).startsWith("HTTP/1.1 200"));
   }
   assertEquals(java.util.Arrays.asList("groups:a:series","start:a:series:Drama"),calls);
   String bad=new JSONObject().put("command","start").put("group","Drama").toString();
   assertTrue(request(port,"POST","/metadata-index",auth,bad,bad.length()).startsWith("HTTP/1.1 400"));
   assertEquals(2,calls.size());
  }
 }

 @Test public void metadataReviewIsAuthenticatedAndValidatesReasonAndCursor()throws Exception {
  java.util.List<String> calls=new java.util.ArrayList<>();
  DeviceWebPanel.Host host=new DeviceWebPanel.Host(){
   public String status(){return "{}";}public long importPlaylist(InputStream in){return 0;}
   public String metadataIndexReview(String state,long after){calls.add(state+":"+after);return "{\"rows\":[]}";}
  };
  try(DeviceWebPanel panel=new DeviceWebPanel(host,new byte[0])){
   int port=panel.start();String body=new JSONObject().put("command","review").put("state","error").put("after",50).toString();
   assertTrue(request(port,"POST","/metadata-index","",body,body.length()).startsWith("HTTP/1.1 401"));
   String login=request(port,"POST","/session","",panel.pin(),6);
   String auth="X-Panel-Token: "+new JSONObject(login.split("\r\n\r\n",2)[1]).getString("token")+"\r\n";
   assertTrue(request(port,"POST","/metadata-index",auth,body,body.length()).startsWith("HTTP/1.1 200"));
   assertEquals(java.util.Arrays.asList("error:50"),calls);
   for(String state:new String[]{"done","skipped","delete"}){
    body=new JSONObject().put("command","review").put("state",state).toString();
    assertTrue(request(port,"POST","/metadata-index",auth,body,body.length()).startsWith("HTTP/1.1 400"));
   }
   body=new JSONObject().put("command","review").put("state","error").put("after",-1).toString();
   assertTrue(request(port,"POST","/metadata-index",auth,body,body.length()).startsWith("HTTP/1.1 400"));
   assertEquals(1,calls.size());
  }
 }
}
