package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class BackendTransportFailurePolicyTest {
    @Test
    public void recognisesWrappedConnectionFailure() {
        assertTrue(BackendTransportFailurePolicy.isTransportFailure(
                new IllegalStateException("request failed", new ConnectException("no route"))
        ));
    }

    @Test
    public void recognisesDnsAndTimeoutFailures() {
        assertTrue(BackendTransportFailurePolicy.isTransportFailure(new UnknownHostException("host")));
        assertTrue(BackendTransportFailurePolicy.isTransportFailure(new SocketTimeoutException("timeout")));
    }

    @Test
    public void doesNotTreatApplicationFailureAsTransportFailure() {
        assertFalse(BackendTransportFailurePolicy.isTransportFailure(
                new IllegalStateException("HTTP 503")
        ));
    }
}
