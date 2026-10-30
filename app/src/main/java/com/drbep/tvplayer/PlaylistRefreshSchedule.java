package com.drbep.tvplayer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Calendar policy only. Does not enqueue jobs or change the existing 12-hour worker.
 * Missed slots collapse into one due decision; no historical replay of imports.
 */
final class PlaylistRefreshSchedule {
    final int startMinute, intervalMinutes, timesPerDay;
    PlaylistRefreshSchedule(int startMinute, int intervalMinutes, int timesPerDay) {
        if(startMinute<0 || startMinute>=1440 || intervalMinutes<30 ||
            intervalMinutes>720 || timesPerDay<1 || timesPerDay>24 ||
            (timesPerDay-1)*intervalMinutes>=1440)
            throw new IllegalArgumentException("Invalid refresh schedule");
        this.startMinute=startMinute;
        this.intervalMinutes=intervalMinutes;
        this.timesPerDay=timesPerDay;
    }

    private SortedSet<Long> slots(LocalDate day, ZoneId zone) {
        SortedSet<Long> result=new TreeSet<>();
        for(int i=0;i<timesPerDay;i++) {
            int minute=(startMinute+i*intervalMinutes)%1440;
            // atZone advances nonexistent spring times and picks the first autumn
            // occurrence. TreeSet collapses two wall slots resolving to one instant.
            result.add(day.atTime(minute/60,minute%60).atZone(zone).toInstant().toEpochMilli());
        }
        return result;
    }

    long nextAfter(long instant, ZoneId zone) {
        LocalDate day=Instant.ofEpochMilli(instant).atZone(zone).toLocalDate();
        for(int offset=0;offset<=2;offset++)
            for(long slot:slots(day.plusDays(offset),zone))if(slot>instant)return slot;
        throw new IllegalStateException("No next refresh slot");
    }

    long latestAtOrBefore(long instant, ZoneId zone) {
        LocalDate day=Instant.ofEpochMilli(instant).atZone(zone).toLocalDate();
        long latest=Long.MIN_VALUE;
        for(int offset=0;offset>=-2;offset--)
            for(long slot:slots(day.plusDays(offset),zone))if(slot<=instant)latest=Math.max(latest,slot);
        if(latest==Long.MIN_VALUE)throw new IllegalStateException("No previous refresh slot");
        return latest;
    }

    boolean due(long lastSuccessfulRefresh, long now, ZoneId zone) {
        return lastSuccessfulRefresh<latestAtOrBefore(now,zone);
    }

    String encode() { return "daily:"+startMinute+":"+intervalMinutes+":"+timesPerDay; }

    static PlaylistRefreshSchedule decode(String value) {
        if(value==null)throw new IllegalArgumentException("Missing refresh schedule");
        String[] parts=value.split(":",-1);
        if(parts.length!=4 || !parts[0].equals("daily"))
            throw new IllegalArgumentException("Invalid refresh schedule");
        try { return new PlaylistRefreshSchedule(Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2]),Integer.parseInt(parts[3])); }
        catch(NumberFormatException invalid){throw new IllegalArgumentException("Invalid refresh schedule",invalid);}
    }
}
