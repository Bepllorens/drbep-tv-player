package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class U7dChannelIdentityTest {
 @Test public void existingChannelsAndAccentsKeepTheirIdentity() {
  assertEquals("la 1", U7dChannelIdentity.normalize("LA 1 HD ISM"));
  assertEquals("vamos", U7dChannelIdentity.normalize("M+ Vamos HD"));
  assertEquals("calle 13", U7dChannelIdentity.normalize("Calle 13 HD"));
  assertEquals("accion", U7dChannelIdentity.normalize("M+ Acción HD"));
  assertEquals("", U7dChannelIdentity.normalize(null));
 }

 @Test public void plusAliasesKeepChannelIdentity() {
  for(String name:new String[]{"Movistar Plus","Movistar Plus HD","MPLUS","M+","Movistar+","M Plus"})
   assertEquals(name,"mplus",U7dChannelIdentity.normalize(name));
 }
 @Test public void otherChannelsKeepDistinctIdentities() {
  assertEquals("estrenos",U7dChannelIdentity.normalize("M+ Estrenos HD"));
  assertEquals("la 1",U7dChannelIdentity.normalize("LA 1 HD"));
  assertEquals("",U7dChannelIdentity.normalize("Movistar ISM"));
  assertNotEquals(U7dChannelIdentity.normalize("Movistar Plus"),U7dChannelIdentity.normalize("M+ Deportes"));
 }
}
