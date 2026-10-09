package com.drbep.tvplayer;
import org.junit.Test;
import java.util.TimeZone;
import static org.junit.Assert.*;
public class LiveDvrClockTest {
 private final TimeZone utc=TimeZone.getTimeZone("UTC");
 private final long midnight=1791504000000L;
 @Test public void selectsPreviousDayAcrossMidnight(){
  long end=midnight+30*60000L;
  assertEquals(midnight-30*60000L,LiveDvrClock.resolve("23:30",end-7200000,end,utc));
 }
 @Test public void rejectsTimeOutsideWindow(){
  long end=midnight+30*60000L;
  assertEquals(-1,LiveDvrClock.resolve("20:00",end-7200000,end,utc));
  assertEquals(-1,LiveDvrClock.resolve("25:00",end-7200000,end,utc));
  assertEquals(-1,LiveDvrClock.resolve("00:99",end-7200000,end,utc));
 }
}
