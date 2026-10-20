package com.drbep.tvplayer;
import java.util.List;
import java.util.Map;

final class CatchupFailurePolicy {
 static final String DENIED_MESSAGE = "Este programa no está autorizado para la cuenta configurada. Puedes volver al directo o elegir otro programa.";
 static boolean isEntitlementDenial(int status, Map<String,List<String>> headers) {
  if(status!=403 || headers==null) return false;
  for(Map.Entry<String,List<String>> entry:headers.entrySet()) {
   if("X-DRBEP-Playback-Error".equalsIgnoreCase(entry.getKey()) && entry.getValue()!=null)
    for(String value:entry.getValue()) if("catchup_not_entitled".equals(value)) return true;
  }
  return false;
 }
}
