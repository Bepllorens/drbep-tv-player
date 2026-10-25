package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Random;

public class LogoContrastTest {
    @Test public void transparencyAndLuminanceThresholdsStayIdentical() {
        int[] p=new int[256];
        assertFalse(LogoContrast.needsHalo(p));
        Arrays.fill(p,0x5F000000); assertFalse(LogoContrast.needsHalo(p));
        Arrays.fill(p,0x60000000); assertTrue(LogoContrast.needsHalo(p));
        Arrays.fill(p,0xFF4A4A4A); assertTrue(LogoContrast.needsHalo(p));
        Arrays.fill(p,0xFFFFFFFF); assertFalse(LogoContrast.needsHalo(p));
    }
    @Test public void haloRequiresMoreThanSixtyFivePercentOfVisiblePixels() {
        int[] p=new int[256];Arrays.fill(p,0xFFFFFFFF);
        Arrays.fill(p,0,166,0xFF000000);assertFalse(LogoContrast.needsHalo(p));
        p[166]=0xFF000000;assertTrue(LogoContrast.needsHalo(p));
        Arrays.fill(p,0);p[0]=0xFF000000;assertTrue(LogoContrast.needsHalo(p));
    }
    @Test public void matchesPreviousDecisionForMixedArgbSamples() {
        Random random=new Random(231023);
        for(int i=0;i<1000;i++) {
            int[] p=new int[256];for(int j=0;j<p.length;j++)p[j]=random.nextInt();
            int visible=0,dark=0;
            for(int pixel:p){
                if((pixel>>>24)<96)continue;
                visible++;
                int r=(pixel>>>16)&255,g=(pixel>>>8)&255,b=pixel&255;
                if(r*.2126+g*.7152+b*.0722<75)dark++;
            }
            assertEquals(visible>0 && dark>visible*.65,LogoContrast.needsHalo(p));
        }
    }
}
