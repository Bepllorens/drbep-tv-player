package com.drbep.tvplayer;
import java.util.*;import java.util.regex.*;import java.text.Normalizer;
import org.junit.Test;import static org.junit.Assert.*;
public class FanartNormalizationEquivalenceTest {
 private static final Pattern EPISODE=Pattern.compile("(?i)\\s*(?:[-:]?\\s*(?:t\\d+\\s*e\\d+|s\\d+e\\d+|ep(?:isodio)?\\.?\\s*\\d+)).*$");
 private static String reference(String s){return Normalizer.normalize(s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}+","").replaceAll("[^a-z0-9]","");}
 private static void check(String s){
  assertEquals(reference(EPISODE.matcher(s).replaceFirst("")),FanartRepository.titleKey(s));
  String channel=s.trim().replaceFirst("(?i)\\s+(?:uhd|fhd|hd|sd|4k)$","").replace("Movistar+","M+").replace("Movistar Plus+","M+");
  assertEquals(reference(channel),FanartRepository.channelKey(s));
  assertEquals(s.matches("[A-Za-z0-9_-]{1,80}"),FanartRepository.validArtworkId(s));
 }
 @Test public void accentsEpisodesAndUnicodeCaseRulesArePreserved(){
  for(String s:new String[]{"M+ Acción HD","Movistar Plus+ Romance UHD","Saber y ganar T1 E5","Saber y ganar - Episodio 5","İ I ı i","ẞ ß STRAẞE","e\u0301 Á ñ","Serie S01E02","Título: ep. 2","ABC_123-Z","","A\nB","A\rB","𐐀 𐐨 😀"})check(s);
 }
 @Test public void generatedUnicodeAndMalformedSurrogatesKeepExactKeys(){
  Random random=new Random(82471);
  for(int n=0;n<4000;n++){StringBuilder s=new StringBuilder();for(int i=0;i<20;i++)s.append((char)random.nextInt(65536));check(s.toString());}
 }
}
