package com.drbep.tvplayer;
import java.time.*;
/** Calendar days, including 23/25 hour daylight-saving transitions. */
final class LiveGuideDay {
 static long start(LocalDate day, ZoneId zone) { return day.atStartOfDay(zone).toInstant().toEpochMilli(); }
 static long end(LocalDate day, ZoneId zone) { return start(day.plusDays(1), zone); }
 static LocalDate move(LocalDate day, int delta, LocalDate today) {
  LocalDate next=day.plusDays(delta), first=today.minusDays(7), last=today.plusDays(7);
  return next.isBefore(first)?first:next.isAfter(last)?last:next;
 }
}
