package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class TouchControlsUiFactoryTest {
    @Test
    public void liveChannelWithU7dKeepsExpectedActionOrder() {
        FakeHost host = new FakeHost();
        host.current = channel(false);
        host.u7dSupported = true;

        TouchControlsBarUiModel model = TouchControlsUiFactory.build(host, 2);

        assertEquals(10, model.actions.size());
        assertLabels(model, "Canales", "Plataforma", "Guia", "U7D", "Anterior", "Info", "Grab", "Rebobinar", "Pausa", "Avanzar");
        assertEquals(2, model.focusedActionIndex);
    }

    @Test
    public void vodChannelUsesVodLibraryAndDetailWithoutLiveOnlyActions() {
        FakeHost host = new FakeHost();
        host.current = channel(true);
        host.u7dSupported = true;

        TouchControlsBarUiModel model = TouchControlsUiFactory.build(host);

        assertLabels(model, "Canales", "Plataforma", "Biblioteca VOD", "Ficha VOD", "Grab", "Rebobinar", "Pausa", "Avanzar");
        assertFalse(hasLabel(model, "U7D"));
        assertFalse(hasLabel(model, "Info"));
        assertFalse(hasLabel(model, "Anterior"));
    }

    @Test
    public void orientationButtonOnlyAppearsWhenSupported() {
        FakeHost host = new FakeHost();
        host.current = channel(false);
        host.orientationSupported = false;

        TouchControlsBarUiModel noRotate = TouchControlsUiFactory.build(host);
        assertFalse(hasLabel(noRotate, "Giro libre"));
        assertFalse(hasLabel(noRotate, "Giro fijo"));

        host.orientationSupported = true;
        host.orientationLocked = true;

        TouchControlsBarUiModel locked = TouchControlsUiFactory.build(host);
        assertTrue(hasLabel(locked, "Giro fijo"));
    }

    @Test
    public void primaryActionsCallExpectedHostCallbacks() {
        FakeHost host = new FakeHost();
        host.current = channel(false);
        host.u7dSupported = true;

        TouchControlsBarUiModel model = TouchControlsUiFactory.build(host);
        click(model, "Canales");
        click(model, "Plataforma");
        click(model, "Guia");
        click(model, "U7D");

        assertEquals("keep,showOverlay,keep,showFilterPicker,keep,openTimelineGuide,keep,openU7D", String.join(",", host.events));
    }

    private static void assertLabels(TouchControlsBarUiModel model, String... labels) {
        assertEquals(labels.length, model.actions.size());
        for (int i = 0; i < labels.length; i++) {
            assertEquals(labels[i], model.actions.get(i).label);
        }
    }

    private static boolean hasLabel(TouchControlsBarUiModel model, String label) {
        for (ZapActionItem item : model.actions) {
            if (label.equals(item.label)) {
                return true;
            }
        }
        return false;
    }

    private static void click(TouchControlsBarUiModel model, String label) {
        for (ZapActionItem item : model.actions) {
            if (label.equals(item.label) && item.onClick != null) {
                item.onClick.run();
                return;
            }
        }
        throw new AssertionError("Missing action " + label);
    }

    private static ChannelItem channel(boolean vod) {
        return new ChannelItem(
                vod ? "vod-1" : "live-1",
                vod ? "VOD 1" : "La 1",
                "",
                "",
                vod ? "VOD" : "TDT",
                "https://stream.example/item",
                "",
                1,
                1,
                vod,
                false,
                1,
                vod ? "Movistar VOD" : "Movistar ISM",
                Collections.emptyList(),
                "",
                "",
                vod ? "vod:movistar" : "",
                true
        );
    }

    private static final class FakeHost implements TouchControlsUiFactory.Host {
        final List<String> events = new ArrayList<>();
        ChannelItem current;
        boolean overlayVisible;
        boolean orientationSupported;
        boolean orientationLocked;
        boolean u7dSupported;

        @Override public String text(int resId) {
            if (resId == R.string.touch_button_list) return "Canales";
            if (resId == R.string.touch_button_platform) return "Plataforma";
            if (resId == R.string.touch_button_guide) return "Guia";
            if (resId == R.string.touch_button_vod_library) return "Biblioteca VOD";
            if (resId == R.string.touch_button_u7d) return "U7D";
            if (resId == R.string.touch_button_previous) return "Anterior";
            if (resId == R.string.touch_button_vod_detail) return "Ficha VOD";
            if (resId == R.string.touch_button_info) return "Info";
            if (resId == R.string.touch_button_recordings) return "Grab";
            if (resId == R.string.touch_button_rotate_locked) return "Giro fijo";
            if (resId == R.string.touch_button_rotate_free) return "Giro libre";
            if (resId == R.string.touch_button_rewind) return "Rebobinar";
            if (resId == R.string.touch_button_play_pause) return "Pausa";
            if (resId == R.string.touch_button_forward) return "Avanzar";
            if (resId == R.string.filter_navigation_hint) return "Filtro";
            return "res:" + resId;
        }

        @Override public String currentFilterLabel() { return "TDT"; }
        @Override public ChannelItem currentChannel() { return current; }
        @Override public boolean isOverlayVisible() { return overlayVisible; }
        @Override public boolean isTabletOrientationLocked() { return orientationLocked; }
        @Override public boolean supportsOrientationLock() { return orientationSupported; }
        @Override public void keepVisible() { events.add("keep"); }
        @Override public void hideOverlay() { events.add("hideOverlay"); overlayVisible = false; }
        @Override public void showOverlay() { events.add("showOverlay"); overlayVisible = true; }
        @Override public void showFilterPicker() { events.add("showFilterPicker"); }
        @Override public void showVodLibrary() { events.add("showVodLibrary"); }
        @Override public void openTimelineGuide() { events.add("openTimelineGuide"); }
        @Override public boolean supportsU7d(ChannelItem item) { return u7dSupported && item != null; }
        @Override public void openU7d(ChannelItem item) { events.add("openU7D"); }
        @Override public void showVodInfo(ChannelItem item) { events.add("showVodInfo"); }
        @Override public void tunePreviousChannel() { events.add("previous"); }
        @Override public void openProgramInfo() { events.add("info"); }
        @Override public void showPlaybackDiagnostics() { events.add("diagnostics"); }
        @Override public void openRecordings() { events.add("recordings"); }
        @Override public void showToolsMenu() { events.add("tools"); }
        @Override public void toggleTabletOrientationLock() { events.add("rotate"); orientationLocked = !orientationLocked; }
        @Override public boolean seekBack() { events.add("seekBack"); return true; }
        @Override public boolean seekForward() { events.add("seekForward"); return true; }
        @Override public void showSeekUnavailable() { events.add("seekUnavailable"); }
        @Override public void togglePlayback() { events.add("togglePlayback"); }
    }
}
