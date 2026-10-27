package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class FanartReadKeysTest {
 @Test public void reusedAndEvictedChannelsKeepCanonicalIdentity() {
  FanartRepository.ReadKeys keys=new FanartRepository.ReadKeys();
  String[] channels={"La 1 HD","M+ Acción HD","Movistar Plus+ Romance UHD","SYFY","DAZN LaLiga","M+ Vamos"," Cañal 4k "};
  for(int n=0;n<5000;n++){
   String c=n%3==0?"Canal "+n:channels[n%channels.length];
   assertEquals(FanartRepository.channelKey(c),keys.channel(c));
   assertTrue(keys.size()<=512);
  }
  for(String c:channels)assertEquals(FanartRepository.channelKey(c),keys.channel(c));
 }
 @Test public void compiledValidatorKeepsExistingAcceptance() {
  String[] ids={"ABC","a_-123","","bad/id","bad.id","é","x\n",repeat('a',80),repeat('a',81)};
  for(String id:ids)assertEquals(id.matches("[A-Za-z0-9_-]{1,80}"),FanartRepository.validArtworkId(id));
 }
 private static String repeat(char c,int n){char[] a=new char[n];java.util.Arrays.fill(a,c);return new String(a);}
}
