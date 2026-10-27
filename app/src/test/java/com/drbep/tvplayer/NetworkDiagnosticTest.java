package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;
public class NetworkDiagnosticTest {
 @Test public void targetNeverIncludesQueryPathOrUserInfo()throws Exception{
  assertEquals("https://example.test/health",NetworkDiagnostic.target("https://example.test/private?token=hidden#frag").toString());
  assertEquals("http://127.0.0.1:8080/health",NetworkDiagnostic.target("http://127.0.0.1:8080/api").toString());
  for(String url:new String[]{"file:///etc/passwd","https://user:secret@example.test/","invalid"}){
   try{NetworkDiagnostic.target(url);fail();}catch(Exception expected){}
  }
 }
 @Test public void timeoutDoesNotReleaseBusySlotOrSpawnAnotherTask()throws Exception{
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  AtomicInteger calls=new AtomicInteger();
  NetworkDiagnostic d=new NetworkDiagnostic(base->{calls.incrementAndGet();entered.countDown();release.await();return new JSONObject();},30,0);
  try {
   assertEquals("timeout",new JSONObject(d.check("https://example.test")).getString("state"));
   assertTrue(entered.await(1,TimeUnit.SECONDS));
   for(int i=0;i<20;i++)assertEquals("busy",new JSONObject(d.check("https://example.test")).getString("state"));
   assertEquals(1,calls.get());
  }finally{release.countDown();}
 }
 @Test public void repeatedRequestsAreThrottledAndErrorsAreSanitized()throws Exception{
  AtomicInteger calls=new AtomicInteger();
  NetworkDiagnostic d=new NetworkDiagnostic(base->{calls.incrementAndGet();throw new Exception("secret-token");},1000,30000);
  String result=d.check("https://example.test");
  assertTrue(result.contains("unavailable"));assertFalse(result.contains("secret"));
  assertTrue(d.check("https://example.test").contains("cooldown"));assertEquals(1,calls.get());
 }
}
