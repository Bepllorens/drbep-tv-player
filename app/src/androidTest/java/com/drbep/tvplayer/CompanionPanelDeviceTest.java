package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.net.*;import java.io.*;import java.util.concurrent.atomic.AtomicReference;
import org.json.*;
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class CompanionPanelDeviceTest {
 @Test public void authenticatedPanelQueriesActualPlayerAndCatchup()throws Exception{
  AtomicReference<MainActivity> activity=new AtomicReference<>();AtomicReference<DeviceWebPanel> panel=new AtomicReference<>();
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   Thread.sleep(12000);
   scenario.onActivity(a->{activity.set(a);try{java.lang.reflect.Method open=MainActivity.class.getDeclaredMethod("showDeviceWebPanel",Runnable.class);open.setAccessible(true);open.invoke(a,(Runnable)()->{});}catch(Exception e){throw new AssertionError(e);}});
   for(int i=0;i<100&&panel.get()==null;i++){Thread.sleep(200);scenario.onActivity(a->{try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField("deviceWebPanel");f.setAccessible(true);panel.set((DeviceWebPanel)f.get(a));}catch(Exception e){throw new AssertionError(e);}});}
   assertNotNull(panel.get());java.lang.reflect.Field f=DeviceWebPanel.class.getDeclaredField("server");f.setAccessible(true);int port=((java.net.ServerSocket)f.get(panel.get())).getLocalPort();
   String token=new JSONObject(post(port,"/session",panel.get().pin(),null)).getString("token");
   JSONObject status=new JSONObject(post(port,"/manage","{\"action\":\"remote-status\"}",token));assertTrue(status.has("seekable"));assertTrue(status.has("canLive"));
   JSONObject catchup=new JSONObject(post(port,"/manage","{\"action\":\"catchup-list\"}",token));assertNotNull(catchup.getJSONArray("rows"));assertNotNull(catchup.getJSONArray("days"));
   assertEquals("No se pudo completar la operación. Comprueba los datos y vuelve a intentarlo.",new JSONObject(post(port,"/manage","{\"action\":\"does-not-exist\"}",token)).getString("error"));
  }finally{if(panel.get()!=null)panel.get().close();}
 }
 private String post(int port,String path,String body,String token)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL("http://127.0.0.1:"+port+path).openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(5000);c.setReadTimeout(20000);if(token!=null)c.setRequestProperty("X-Panel-Token",token);byte[] bytes=body.getBytes("UTF-8");c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}try(InputStream in=c.getResponseCode()<400?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toString("UTF-8");}finally{c.disconnect();}
 }
}
