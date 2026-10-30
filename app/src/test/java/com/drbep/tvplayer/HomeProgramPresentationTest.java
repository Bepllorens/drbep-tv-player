package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class HomeProgramPresentationTest {
 @Test public void preservesLiveRoundSeasonAndUnicodePresentation() {
  assertEquals("",HomeProgramPresentation.of(null).title);
  assertEquals("",HomeProgramPresentation.of("   ").metadata);
  assertEquals("Águila y ñandú",HomeProgramPresentation.of("  DIRECTO Águila y ñandú  ").title);
  HomeProgramPresentation p=HomeProgramPresentation.of("Directo Jornada 12: Real Madrid - Barça T26/27 · Liga");
  assertEquals("Real Madrid - Barça",p.title);
  assertEquals("Liga · Jornada 12 · T26/27",p.metadata);
  assertEquals("DIRECTOR de cine",HomeProgramPresentation.of("DIRECTOR de cine").title);
  assertEquals("Jornada especial",HomeProgramPresentation.of("Jornada especial").title);
  assertEquals("Serie T1/2",HomeProgramPresentation.of("Serie T1/2").title);
 }
 @Test public void sharedPatternsKeepIndependentConcurrentMatchers() throws Exception {
  java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(4);
  try {
   java.util.List<java.util.concurrent.Future<?>> results=new java.util.ArrayList<>();
   for(int t=0;t<8;t++) {final int id=t;results.add(pool.submit(()->{
    for(int n=0;n<500;n++){
     HomeProgramPresentation p=HomeProgramPresentation.of("DIRECTO Jornada "+id+": Equipo "+n+" T26/27");
     assertEquals("Equipo "+n,p.title);assertEquals("Jornada "+id+" · T26/27",p.metadata);
    }
   }));}
   for(java.util.concurrent.Future<?> f:results)f.get();
  } finally {pool.shutdownNow();}
 }
}
