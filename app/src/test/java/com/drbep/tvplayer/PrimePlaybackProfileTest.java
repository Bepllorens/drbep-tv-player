package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PrimePlaybackProfileTest {
    @Test
    public void amazonDevicesRequestUhdProfile() {
        assertEquals("android-uhd", PlayerController.primePlaybackProfileForManufacturer("Amazon"));
    }

    @Test
    public void otherAndroidDevicesKeepCompatibleProfile() {
        assertEquals("software", PlayerController.primePlaybackProfileForManufacturer("Xiaomi"));
        assertEquals("software", PlayerController.primePlaybackProfileForManufacturer(null));
    }
}
