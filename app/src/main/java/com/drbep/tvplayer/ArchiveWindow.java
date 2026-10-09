package com.drbep.tvplayer;
import java.time.*;import java.util.*;
final class ArchiveWindow {
 static Set<String> days(String selected,long now){LocalDate today=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();Set<String> out=new HashSet<>();out.add(today.toString());out.add(today.minusDays(1).toString());try{LocalDate d=LocalDate.parse(selected);if(!d.isAfter(today)&&!d.isBefore(today.minusDays(7)))out.add(d.toString());}catch(Exception ignored){}return out;}
 static String day(String value){try{return OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate().toString();}catch(Exception e){try{return LocalDateTime.parse(value.replace(' ','T')).toLocalDate().toString();}catch(Exception ignored){return "";}}}
 static boolean retained(String value,long now){String d=day(value);LocalDate today=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();return !d.isEmpty()&&d.compareTo(today.minusDays(7).toString())>=0&&d.compareTo(today.toString())<=0;}
 static java.util.List<EpgRepository.EpgProgram> filter(java.util.List<EpgRepository.EpgProgram> items,Set<String> days){java.util.List<EpgRepository.EpgProgram> out=new ArrayList<>();for(EpgRepository.EpgProgram p:items)if(days.contains(day(p.startTime)))out.add(p);return out;}
}
