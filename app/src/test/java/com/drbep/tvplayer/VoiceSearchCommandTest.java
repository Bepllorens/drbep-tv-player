package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class VoiceSearchCommandTest {
 @Test public void guideUnderstandsAccentsAndPunctuation() {
  VoiceSearchCommand c=VoiceSearchCommand.parse("¿Qué echan en La 1?");
  assertEquals(VoiceSearchCommand.Kind.GUIDE,c.kind);assertEquals("la 1",c.query);
 }
 @Test public void playRequiresExplicitVerb() {
  assertEquals(VoiceSearchCommand.Kind.PLAY,VoiceSearchCommand.parse("Pon el canal La 1").kind);
  assertEquals(VoiceSearchCommand.Kind.SEARCH,VoiceSearchCommand.parse("La 1").kind);
 }
 @Test public void recommendationsUseGenreAndSimilarRemainsSeparate() {
  assertEquals("Romance",VoiceSearchCommand.parse("Algo de amor").query);
  assertEquals("Acción",VoiceSearchCommand.parse("Películas de acción").query);
  assertEquals(VoiceSearchCommand.Kind.SIMILAR,VoiceSearchCommand.parse("Parecidas a Torrente").kind);
 }
 @Test public void ordinaryTitlesAndEmptyInputStaySearches() {
  assertEquals("Qué bello es vivir",VoiceSearchCommand.parse("Qué bello es vivir").query);
  assertEquals("",VoiceSearchCommand.parse(null).query);
  assertEquals(VoiceSearchCommand.Kind.SEARCH,VoiceSearchCommand.parse("pon ").kind);
 }
}
