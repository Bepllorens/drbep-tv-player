package com.drbep.tvplayer;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public class TimelineProgrammeDetailsTest {
 @Test public void matchesExactChannelAndStartNotSameTitle()throws Exception {
  JSONArray rows=new JSONArray("[[{\"Canal\":{\"CodCadenaTv\":\"A\"},\"FechaHoraInicio\":100,\"Ficha\":\"wrong\"},{\"Canal\":{\"CodCadenaTv\":\"B\"},\"FechaHoraInicio\":100,\"Ficha\":\"right\"}]]");
  assertEquals("right",TimelineProgrammeDetails.findFicha(rows,"B",100));
  assertEquals("",TimelineProgrammeDetails.findFicha(rows,"B",101));
 }
 @Test public void onlyPublicMetadataEndpointAllowed(){
  assertTrue(TimelineProgrammeDetails.allowedFicha("https://ottcache.dof6.com/movistarplus/webplayer/contents/123/details?profile=OTT"));
  assertFalse(TimelineProgrammeDetails.allowedFicha("https://example.com/movistarplus/webplayer/contents/123/details"));
  assertFalse(TimelineProgrammeDetails.allowedFicha("https://ottcache.dof6.com/licence"));
  assertFalse(TimelineProgrammeDetails.allowedFicha("http://ottcache.dof6.com/movistarplus/webplayer/contents/123/details"));
 }
 @Test public void descriptionFallsBackWhenPrimaryEmpty()throws Exception{
  assertEquals("Summary",TimelineProgrammeDetails.description(new JSONObject().put("Descripcion", " ").put("Sinopsis","Summary")));
  assertEquals("Full",TimelineProgrammeDetails.description(new JSONObject().put("Descripcion","Full").put("Sinopsis","Short")));
 }
}
