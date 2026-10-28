package com.drbep.tvplayer;
import java.time.*;import org.junit.Test;import static org.junit.Assert.*;
public class LiveGuideDayTest {
 @Test public void daylightSavingUsesCalendarDays() {
  ZoneId z=ZoneId.of("Europe/Madrid");
  LocalDate spring=LocalDate.of(2026,3,29),fall=LocalDate.of(2026,10,25);
  assertEquals(23*3600000L,LiveGuideDay.end(spring,z)-LiveGuideDay.start(spring,z));
  assertEquals(25*3600000L,LiveGuideDay.end(fall,z)-LiveGuideDay.start(fall,z));
  assertEquals(LiveGuideDay.end(spring,z),LiveGuideDay.start(spring.plusDays(1),z));
 }
 @Test public void navigationIsBoundedAndReversible() {
  LocalDate today=LocalDate.of(2026,10,28);
  assertEquals(today.minusDays(7),LiveGuideDay.move(today.minusDays(7),-1,today));
  assertEquals(today.plusDays(7),LiveGuideDay.move(today.plusDays(7),1,today));
  assertEquals(today,LiveGuideDay.move(LiveGuideDay.move(today,-1,today),1,today));
 }
}
