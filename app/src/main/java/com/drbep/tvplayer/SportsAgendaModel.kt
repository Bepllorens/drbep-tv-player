package com.drbep.tvplayer

import org.json.JSONArray
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId

internal object SportsAgendaModel {
 fun timestamp(raw:String):java.time.Instant? = EpgTimeCodec.parseEpochMillis(raw).takeIf{it>0}?.let(java.time.Instant::ofEpochMilli)
 fun window(day:LocalDate,zone:ZoneId)=day.atStartOfDay(zone).toInstant() to day.plusDays(1).atStartOfDay(zone).toInstant()
 fun preferenceKey(scope:String):String="sports_follow_v1_"+MessageDigest.getInstance("SHA-256").digest(scope.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
 fun normalize(value:String)=value.trim().replace(Regex("\\s+")," ").take(80)
 fun decode(raw:String):List<String> = runCatching {
  val a=JSONArray(raw);(0 until minOf(a.length(),32)).mapNotNull { (a.opt(it) as? String)?.let(::normalize)?.takeIf(String::isNotEmpty) }.distinctBy{it.lowercase(java.util.Locale.ROOT)}
 }.getOrDefault(emptyList())
 fun encode(values:List<String>)=JSONArray(values.map(::normalize).filter(String::isNotEmpty).distinctBy{it.lowercase(java.util.Locale.ROOT)}.take(32)).toString()
 fun toggle(values:List<String>,value:String):List<String>{
  val term=normalize(value);if(term.isEmpty())return values
  return if(values.any{it.equals(term,true)})values.filterNot{it.equals(term,true)}else if(values.size<32)values+term else values
 }
}
