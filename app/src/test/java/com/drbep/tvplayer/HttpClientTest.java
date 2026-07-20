package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class HttpClientTest {
    @Test
    public void readAllAcceptsResponseAtLimit() throws Exception {
        byte[] body = "respuesta".getBytes(StandardCharsets.UTF_8);

        assertEquals("respuesta", HttpClient.readAll(new ByteArrayInputStream(body), body.length));
    }

    @Test
    public void readAllRejectsResponseAboveLimit() {
        byte[] body = "respuesta demasiado grande".getBytes(StandardCharsets.UTF_8);

        assertThrows(
                IllegalStateException.class,
                () -> HttpClient.readAll(new ByteArrayInputStream(body), body.length - 1)
        );
    }
}
