package com.drbep.tvplayer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppUpdateManagerUrlTest {
    @Test
    public void emergencyMetadataRebasesPrimaryApkUrlToVps() {
        assertEquals(
                "https://direct.tvbep.com/api/offline/app/apk?channel=beta",
                AppUpdateManager.resolveDownloadUrl(
                        "https://direct.tvbep.com",
                        "https://fire.tvbep.com/api/offline/app/apk?channel=beta"
                )
        );
    }

    @Test
    public void relativeApkUrlUsesMetadataSource() {
        assertEquals(
                "https://direct.tvbep.com/api/offline/app/apk?channel=beta",
                AppUpdateManager.resolveDownloadUrl(
                        "https://direct.tvbep.com",
                        "/api/offline/app/apk?channel=beta"
                )
        );
    }
}
