package com.drbep.tvplayer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;

/** Hardware-informed limits; input type and screen size remain separate concerns. */
final class DevicePerformanceProfile {
    enum FormFactor { TV, PHONE, TABLET }

    final FormFactor formFactor;
    final boolean lowRam;
    final int memoryClassMb;
    final int maxMultiViewStreams;

    private DevicePerformanceProfile(FormFactor formFactor, boolean lowRam, int memoryClassMb, int maxMultiViewStreams) {
        this.formFactor = formFactor;
        this.lowRam = lowRam;
        this.memoryClassMb = memoryClassMb;
        this.maxMultiViewStreams = maxMultiViewStreams;
    }

    static DevicePerformanceProfile detect(Context context) {
        PackageManager packageManager = context.getPackageManager();
        boolean tv = packageManager != null && packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK);
        Configuration configuration = context.getResources().getConfiguration();
        boolean tablet = configuration.smallestScreenWidthDp >= 600;
        FormFactor formFactor = tv ? FormFactor.TV : tablet ? FormFactor.TABLET : FormFactor.PHONE;

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        boolean lowRam = activityManager != null && activityManager.isLowRamDevice();
        int memoryClassMb = activityManager == null ? 192 : activityManager.getMemoryClass();
        int streams = lowRam || memoryClassMb < 256 ? 2 : memoryClassMb < 384 ? 3 : 4;
        return new DevicePerformanceProfile(formFactor, lowRam, memoryClassMb, streams);
    }
}
