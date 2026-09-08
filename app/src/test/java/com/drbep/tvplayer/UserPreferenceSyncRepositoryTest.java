package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UserPreferenceSyncRepositoryTest {
    @Test
    public void preferenceEndpointNormalizesTrailingSlash() {
        assertEquals(
                "https://fire.tvbep.com/api/offline/preferences",
                UserPreferenceSyncRepository.endpoint("https://fire.tvbep.com/")
        );
    }
}
