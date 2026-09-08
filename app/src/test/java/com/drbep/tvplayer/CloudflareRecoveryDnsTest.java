package com.drbep.tvplayer;

import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Dns;

import static org.junit.Assert.assertEquals;

public class CloudflareRecoveryDnsTest {
    @Test
    public void protectedHostKeepsSystemFirstAndAppendsRecoveryAddresses() throws Exception {
        InetAddress normal = InetAddress.getByName("188.114.96.5");
        InetAddress recovered = InetAddress.getByName("104.21.59.41");
        CloudflareRecoveryDns dns = new CloudflareRecoveryDns(
                host -> Collections.singletonList(normal),
                host -> Arrays.asList(normal, recovered)
        );

        assertEquals(Arrays.asList(normal, recovered), dns.lookup("fire.tvbep.com"));
    }

    @Test
    public void protectedHostTriesRecoveryBeforeRemainingSystemAddresses() throws Exception {
        InetAddress normalOne = InetAddress.getByName("188.114.96.5");
        InetAddress normalTwo = InetAddress.getByName("188.114.97.5");
        InetAddress recovered = InetAddress.getByName("104.21.59.41");
        CloudflareRecoveryDns dns = new CloudflareRecoveryDns(
                host -> Arrays.asList(normalOne, normalTwo),
                host -> Collections.singletonList(recovered)
        );

        assertEquals(
                Arrays.asList(normalOne, recovered, normalTwo),
                dns.lookup("fire.tvbep.com")
        );
    }

    @Test
    public void protectedHostUsesRecoveryWhenSystemResolutionFails() throws Exception {
        InetAddress recovered = InetAddress.getByName("172.67.213.122");
        CloudflareRecoveryDns dns = new CloudflareRecoveryDns(
                host -> { throw new UnknownHostException(host); },
                host -> Collections.singletonList(recovered)
        );

        assertEquals(Collections.singletonList(recovered), dns.lookup("fire.tvbep.com"));
    }

    @Test
    public void canaryBuildCanPreferRecoveryWithoutDroppingSystemFallback() throws Exception {
        InetAddress normal = InetAddress.getByName("188.114.96.5");
        InetAddress recovered = InetAddress.getByName("104.21.59.41");
        CloudflareRecoveryDns dns = new CloudflareRecoveryDns(
                host -> Collections.singletonList(normal),
                host -> Collections.singletonList(recovered),
                true
        );

        assertEquals(Arrays.asList(recovered, normal), dns.lookup("fire.tvbep.com"));
    }

    @Test
    public void unrelatedHostNeverCallsRecoveryResolver() throws Exception {
        InetAddress normal = InetAddress.getByName("203.0.113.8");
        AtomicInteger recoveryCalls = new AtomicInteger();
        Dns recovery = host -> {
            recoveryCalls.incrementAndGet();
            return Collections.emptyList();
        };
        CloudflareRecoveryDns dns = new CloudflareRecoveryDns(
                host -> Collections.singletonList(normal),
                recovery
        );

        assertEquals(Collections.singletonList(normal), dns.lookup("example.com"));
        assertEquals(0, recoveryCalls.get());
    }
}
