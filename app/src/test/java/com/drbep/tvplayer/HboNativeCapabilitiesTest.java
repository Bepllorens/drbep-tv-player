package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class HboNativeCapabilitiesTest {
 @Test public void uhdRequiresNativeL1AndSecureMain10Decoder() {
  assertTrue(HboNativeCapabilities.supportsUhd("L1",true));
  assertFalse(HboNativeCapabilities.supportsUhd("L3",true));
  assertFalse(HboNativeCapabilities.supportsUhd("L1",false));
  assertFalse(HboNativeCapabilities.supportsUhd(null,true));
 }
}
