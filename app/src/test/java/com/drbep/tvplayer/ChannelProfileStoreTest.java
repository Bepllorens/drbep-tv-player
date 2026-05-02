package com.drbep.tvplayer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChannelProfileStoreTest {
    @Test
    public void aliasOverridesDisplayNameAndCanBeCleared() {
        ChannelProfileStore store = new ChannelProfileStore(null, "profiles");

        store.setAlias("ch-1", "Mi canal");

        assertTrue(store.hasAlias("ch-1"));
        assertEquals("Mi canal", store.getDisplayName("ch-1", "Canal original"));

        store.setAlias("ch-1", "");

        assertFalse(store.hasAlias("ch-1"));
        assertEquals("Canal original", store.getDisplayName("ch-1", "Canal original"));
    }

    @Test
    public void hiddenChannelsAreTracked() {
        ChannelProfileStore store = new ChannelProfileStore(null, "profiles");

        store.setHidden("ch-1", true);

        assertTrue(store.isHidden("ch-1"));
        assertTrue(store.getHiddenChannelIds().contains("ch-1"));

        store.setHidden("ch-1", false);

        assertFalse(store.isHidden("ch-1"));
        assertFalse(store.getHiddenChannelIds().contains("ch-1"));
    }

    @Test
    public void tagCanBeSetAndCleared() {
        ChannelProfileStore store = new ChannelProfileStore(null, "profiles");

        store.setTag("ch-1", "Casa");

        assertTrue(store.hasTag("ch-1"));
        assertEquals("Casa", store.getTag("ch-1"));

        store.setTag("ch-1", "");

        assertFalse(store.hasTag("ch-1"));
        assertEquals("", store.getTag("ch-1"));
    }
}
