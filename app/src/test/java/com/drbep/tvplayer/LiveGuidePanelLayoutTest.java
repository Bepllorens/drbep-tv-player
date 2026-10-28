package com.drbep.tvplayer;
import org.junit.Test;import static org.junit.Assert.*;
public class LiveGuidePanelLayoutTest {
 @Test public void leavesVideoVisibleOnPhoneTabletAndTv(){
  for(int width:new int[]{320,360,600,960,1280,1920}){
   int rail=LiveGuidePanelLayout.rail(width),guide=LiveGuidePanelLayout.guide(width);
   assertTrue(rail>0&&guide>0);assertTrue(rail+guide<=width*.77);
   assertTrue(guide<=320);assertTrue(rail<=64);
  }
 }
 @Test public void tinyWindowDoesNotGoNegative(){assertEquals(0,LiveGuidePanelLayout.guide(0));}
}
