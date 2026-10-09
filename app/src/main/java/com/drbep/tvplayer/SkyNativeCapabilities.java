package com.drbep.tvplayer;

import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaDrm;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import java.util.UUID;

final class SkyNativeCapabilities {
    static boolean allowsUhd(String security, boolean hdcp22, boolean displayUhd, boolean secureHevc) {
        return "L1".equals(security) && hdcp22 && displayUhd && secureHevc;
    }
    static boolean supportsUhd(Context context) {
        if (Build.VERSION.SDK_INT < 28) return false;
        try (MediaDrm drm=new MediaDrm(UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"))) {
            String security=drm.getPropertyString("securityLevel");
            int hdcp=drm.getConnectedHdcpLevel();
            boolean hdcp22=hdcp>=MediaDrm.HDCP_V2_2 && hdcp<=MediaDrm.HDCP_V2_3;
            WindowManager wm=(WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            Display.Mode mode=wm.getDefaultDisplay().getMode();
            boolean displayUhd=mode.getPhysicalWidth()>=3840 && mode.getPhysicalHeight()>=2160;
            boolean secureHevc=false;
            for(MediaCodecInfo codec:new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos()) {
                if(codec.isEncoder() || (Build.VERSION.SDK_INT>=29 && !codec.isHardwareAccelerated()))continue;
                for(String mime:codec.getSupportedTypes()) {
                    if(!"video/hevc".equalsIgnoreCase(mime))continue;
                    MediaCodecInfo.CodecCapabilities caps=codec.getCapabilitiesForType(mime);
                    boolean main10=false;
                    for(MediaCodecInfo.CodecProfileLevel profile:caps.profileLevels)
                        main10 |= profile.profile==MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10;
                    secureHevc |= main10 && caps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_SecurePlayback)
                            && caps.getVideoCapabilities().areSizeAndRateSupported(3840,2160,30);
                }
            }
            boolean supported=allowsUhd(security,hdcp22,displayUhd,secureHevc);
            Log.w("SkyNativeCDM","security="+security+" hdcp="+hdcp+" displayUhd="+displayUhd
                    +" secureHevc="+secureHevc+" requestUhd="+supported);
            return supported;
        }catch(Exception unavailable){return false;}
    }
}
