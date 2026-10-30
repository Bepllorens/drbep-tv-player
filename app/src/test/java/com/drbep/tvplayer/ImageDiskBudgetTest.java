package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class ImageDiskBudgetTest {
 @Test public void boundedForSmallAndUnknownDisks() {
  assertEquals(32*ImageDiskBudget.MIB,ImageDiskBudget.glideBytes(0));
  assertEquals(32*ImageDiskBudget.MIB,ImageDiskBudget.glideBytes(-1));
  assertEquals(32*ImageDiskBudget.MIB,ImageDiskBudget.glideBytes(100*ImageDiskBudget.MIB));
 }
 @Test public void growsWithoutExceedingExistingCeiling() {
  assertEquals(40*ImageDiskBudget.MIB,ImageDiskBudget.glideBytes(2000*ImageDiskBudget.MIB));
  assertEquals(128*ImageDiskBudget.MIB,ImageDiskBudget.glideBytes(Long.MAX_VALUE));
 }
}
