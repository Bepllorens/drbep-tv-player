package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class HomePreviewQualityPolicyTest {
    @Test public void previewCapsUhdWithoutChangingFullScreen() {
        assertArrayEquals(new int[]{1280,720,2500000}, HomePreviewQualityPolicy.limits(true,3840,2160,20000000));
        assertArrayEquals(new int[]{3840,2160,20000000}, HomePreviewQualityPolicy.limits(false,3840,2160,20000000));
    }
    @Test public void previewPreservesStricterUserLimits() {
        assertArrayEquals(new int[]{640,360,1000000}, HomePreviewQualityPolicy.limits(true,640,360,1000000));
    }
}
