package com.drbep.tvplayer
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class SportsAgendaModelTest {
 @Test fun timestampsAcceptProviderOffsets(){
  assertEquals(Instant.parse("2026-10-27T12:00:00Z"),SportsAgendaModel.timestamp("2026-10-27T13:00:00+01:00"))
  assertNull(SportsAgendaModel.timestamp("invalid"))
 }
 @Test fun localDaysRespectBothClockChanges(){
  val zone=ZoneId.of("Europe/Madrid")
  val spring=SportsAgendaModel.window(LocalDate.of(2026,3,29),zone)
  val fall=SportsAgendaModel.window(LocalDate.of(2026,10,25),zone)
  assertEquals(23,Duration.between(spring.first,spring.second).toHours())
  assertEquals(25,Duration.between(fall.first,fall.second).toHours())
 }
 @Test fun favoritesRoundTripAreBoundedAndCaseInsensitive(){
  val values=SportsAgendaModel.toggle(emptyList(),"  Real   Madrid ")
  assertEquals(listOf("Real Madrid"),values)
  assertEquals(values,SportsAgendaModel.decode(SportsAgendaModel.encode(values)))
  assertTrue(SportsAgendaModel.toggle(values,"real madrid").isEmpty())
  assertEquals(32,SportsAgendaModel.decode(SportsAgendaModel.encode((1..50).map{"Team $it"})).size)
  assertEquals(80,SportsAgendaModel.normalize("x".repeat(200)).length)
 }
 @Test fun corruptStorageAndUnknownValuesDoNotCrash(){
  assertTrue(SportsAgendaModel.decode("{").isEmpty())
  assertEquals(listOf("Fútbol"),SportsAgendaModel.decode("[null,12,\"Fútbol\",\"fútbol\",\"\"]"))
 }
 @Test fun accountsAndServersNeverShareFavorites(){
  val a=SportsAgendaModel.preferenceKey("serverA|alice")
  assertEquals(a,SportsAgendaModel.preferenceKey("serverA|alice"))
  assertNotEquals(a,SportsAgendaModel.preferenceKey("serverA|bob"))
  assertNotEquals(a,SportsAgendaModel.preferenceKey("serverB|alice"))
  assertFalse(a.contains("alice"))
 }
}
