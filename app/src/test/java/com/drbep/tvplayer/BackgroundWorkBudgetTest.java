package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class BackgroundWorkBudgetTest {
 @Test public void quietPeriodExpiresWithoutAPlaybackCallback() {
  BackgroundWorkBudget b=new BackgroundWorkBudget();b.defer(100,8000);
  assertEquals(8000,b.remaining(100));assertEquals(0,b.remaining(8100));
 }
 @Test public void InputCannotShortenStartupProtection() {
  BackgroundWorkBudget b=new BackgroundWorkBudget();b.defer(100,8000);b.defer(200,750);
  assertEquals(7900,b.remaining(200));b.defer(8000,750);assertEquals(650,b.remaining(8100));
 }
}
