package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProgramArtworkResolverTest {
    @Test
    public void prefersProgramArtworkWhenAvailable() {
        assertEquals(
                "https://images.example/program.jpg",
                ProgramArtworkResolver.resolve(" https://images.example/program.jpg ", "https://images.example/channel.png")
        );
    }

    @Test
    public void fallsBackToChannelLogoWhenProgramArtworkIsMissing() {
        assertEquals(
                "https://images.example/channel.png",
                ProgramArtworkResolver.resolve("  ", " https://images.example/channel.png ")
        );
    }

    @Test
    public void ignoresPlaceholderArtworkValues() {
        assertEquals(
                "https://images.example/channel.png",
                ProgramArtworkResolver.resolve("undefined", "https://images.example/channel.png")
        );
        assertEquals(
                "https://images.example/channel.png",
                ProgramArtworkResolver.resolve("data:", "https://images.example/channel.png")
        );
        assertEquals(
                "https://images.example/channel.png",
                ProgramArtworkResolver.resolve("about:blank", "https://images.example/channel.png")
        );
    }

    @Test
    public void returnsEmptyWhenBothSourcesAreMissing() {
        assertEquals("", ProgramArtworkResolver.resolve((String) null, null));
    }
}
