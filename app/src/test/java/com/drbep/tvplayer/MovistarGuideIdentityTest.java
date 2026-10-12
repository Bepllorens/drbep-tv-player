package com.drbep.tvplayer;
import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;
public class MovistarGuideIdentityTest {
 private JSONObject channel(String code,String name,String service)throws Exception{return new JSONObject().put("CodCadenaTv",code).put("Nombre",name).put("ServiceUid2",service);}
 @Test public void resolvesLigaChannelsByExactIdentity()throws Exception{
  JSONObject dazn=channel("DAZNLI","DAZN LALIGA","5119"),liga=channel("MLIGA","M+ LALIGA","4882"),dazn2=channel("DAZNL2","DAZN LALIGA 2","5120");
  List<JSONObject> rows=Arrays.asList(dazn,liga,dazn2);
  assertSame(liga,MovistarGuideRepository.resolve(rows,"M+ LaLiga HD",""));
  assertSame(dazn,MovistarGuideRepository.resolve(rows,"DAZN LaLiga",""));
  assertSame(dazn2,MovistarGuideRepository.resolve(rows,"DAZN LaLiga 2",""));
  assertSame(liga,MovistarGuideRepository.resolve(rows,"Nombre personalizado","","4882"));
  assertSame(dazn,MovistarGuideRepository.resolve(rows,"Nombre personalizado","DAZNLI"));
  assertNull(MovistarGuideRepository.resolve(rows,"Liga",""));
 }
 @Test public void ambiguousNameDoesNotSelectFirstChannel()throws Exception{
  assertNull(MovistarGuideRepository.resolve(Arrays.asList(channel("A","Canal","1"),channel("B","Canal","2")),"Canal",""));
 }
}
