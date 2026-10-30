package com.drbep.tvplayer;
final class ActivePlaybackReturnPolicy {
 static boolean allows(String expected,String current,boolean active,boolean permitted) {
  return permitted && active && expected!=null && !expected.isEmpty() && expected.equals(current);
 }
}
