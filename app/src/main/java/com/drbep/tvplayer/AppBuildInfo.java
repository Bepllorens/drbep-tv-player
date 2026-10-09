package com.drbep.tvplayer;
/** Runtime bridge avoids stale inlined version labels in incremental Kotlin builds. */
public final class AppBuildInfo { private AppBuildInfo() {} public static int versionCode() { return BuildConfig.VERSION_CODE; } }
