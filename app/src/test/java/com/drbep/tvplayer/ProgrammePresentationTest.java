package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.JSONObject;
import java.util.TimeZone;
public class ProgrammePresentationTest {
 @Test public void dstDaysUseLocalMidnightNotFixed24Hours()throws Exception{
  TimeZone previous=TimeZone.getDefault();try{TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"));
   long[] spring=PlaylistGuidePanel.dayRange("2026-03-29"),autumn=PlaylistGuidePanel.dayRange("2026-10-25");
   assertEquals(23*3600000L,spring[1]-spring[0]);assertEquals(25*3600000L,autumn[1]-autumn[0]);
  }finally{TimeZone.setDefault(previous);}
 }
 @Test public void genresAndStatesDoNotDependOnPlayback(){assertEquals("#68C990",ProgrammePresentation.color("Fútbol"));assertEquals("#83B9F9",ProgrammePresentation.color("Informativos"));assertEquals("En directo",ProgrammePresentation.state(10,20,15));assertEquals("Emitido",ProgrammePresentation.state(10,20,20));}
 @Test public void missingMetadataDoesNotProduceEmptySeparators()throws Exception {assertEquals("",ProgrammePresentation.metadata(new JSONObject()));assertEquals("Drama · 2026",ProgrammePresentation.metadata(new JSONObject().put("genre","Drama").put("year","2026")));}
}
