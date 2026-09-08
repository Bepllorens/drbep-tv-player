package com.drbep.tvplayer;

import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;

/**
 * Keeps one system DNS route first, then independently resolved Cloudflare
 * addresses, and finally any remaining system routes. This preserves the normal
 * route while bounding the cost of a blocked address before trying recovery,
 * without changing URL, Host or TLS SNI.
 */
final class CloudflareRecoveryDns implements Dns {
    static final String PRIMARY_HOST = "fire.tvbep.com";
    static final String RECOVERY_DOH_URL = "https://lite.xdp.es/dns-query";

    private static final String TAG = "CloudflareRecoveryDns";
    private static final long RECOVERY_CACHE_MS = 30_000L;

    private final Dns systemDns;
    private final Dns recoveryDns;
    private final boolean recoveryFirst;
    private volatile CacheEntry recoveryCache;

    CloudflareRecoveryDns(Dns systemDns, Dns recoveryDns) {
        this(systemDns, recoveryDns, false);
    }

    CloudflareRecoveryDns(Dns systemDns, Dns recoveryDns, boolean recoveryFirst) {
        this.systemDns = systemDns == null ? Dns.SYSTEM : systemDns;
        this.recoveryDns = recoveryDns;
        this.recoveryFirst = recoveryFirst;
    }

    static CloudflareRecoveryDns createDefault() {
        OkHttpClient bootstrapClient = new OkHttpClient.Builder()
                .dns(Dns.SYSTEM)
                .connectTimeout(java.time.Duration.ofSeconds(3))
                .readTimeout(java.time.Duration.ofSeconds(4))
                .retryOnConnectionFailure(true)
                .build();
        DnsOverHttps recoveryDns = new DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url(HttpUrl.get(RECOVERY_DOH_URL))
                .includeIPv6(true)
                .post(true)
                .build();
        return new CloudflareRecoveryDns(
                Dns.SYSTEM,
                recoveryDns,
                BuildConfig.DNS_HEALING_RECOVERY_FIRST
        );
    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        if (!isProtectedHost(hostname)) {
            return systemDns.lookup(hostname);
        }

        UnknownHostException systemError = null;
        List<InetAddress> systemAddresses = new ArrayList<>();
        try {
            systemAddresses.addAll(systemDns.lookup(hostname));
        } catch (UnknownHostException error) {
            systemError = error;
        }

        List<InetAddress> recoveryAddresses = lookupRecovery(hostname);
        List<InetAddress> merged = recoveryFirst
                ? mergeDistinct(recoveryAddresses, systemAddresses)
                : interleaveRecoveryAfterFirstSystem(systemAddresses, recoveryAddresses);
        if (merged.isEmpty()) {
            if (systemError != null) {
                throw systemError;
            }
            throw new UnknownHostException(hostname);
        }

        int added = Math.max(0, merged.size() - distinctSize(systemAddresses));
        safeLogInfo("resolved host=" + PRIMARY_HOST
                + " system=" + systemAddresses.size()
                + " recovery=" + recoveryAddresses.size()
                + " additional=" + added
                + " recoveryFirst=" + recoveryFirst);
        return merged;
    }

    boolean isRecoveryAddress(InetAddress address) {
        CacheEntry cached = recoveryCache;
        if (address == null || cached == null) {
            return false;
        }
        String target = address.getHostAddress();
        for (InetAddress candidate : cached.addresses) {
            if (candidate != null && candidate.getHostAddress().equals(target)) {
                return true;
            }
        }
        return false;
    }

    private List<InetAddress> lookupRecovery(String hostname) {
        if (recoveryDns == null) {
            return new ArrayList<>();
        }
        long now = System.currentTimeMillis();
        CacheEntry cached = recoveryCache;
        if (cached != null && now - cached.loadedAtMs < RECOVERY_CACHE_MS) {
            return new ArrayList<>(cached.addresses);
        }
        try {
            List<InetAddress> resolved = recoveryDns.lookup(hostname);
            List<InetAddress> clean = mergeDistinct(new ArrayList<>(), resolved);
            if (!clean.isEmpty()) {
                recoveryCache = new CacheEntry(now, clean);
            }
            return clean;
        } catch (Exception error) {
            safeLogWarning("recovery DNS unavailable; keeping system DNS", error);
            return cached == null ? new ArrayList<>() : new ArrayList<>(cached.addresses);
        }
    }

    private static void safeLogInfo(String message) {
        try {
            Log.w(TAG, message);
        } catch (RuntimeException ignored) {
            // android.util.Log is not available in plain JVM unit tests.
        }
    }

    private static void safeLogWarning(String message, Throwable error) {
        try {
            Log.w(TAG, message, error);
        } catch (RuntimeException ignored) {
            // android.util.Log is not available in plain JVM unit tests.
        }
    }

    static boolean isProtectedHost(String hostname) {
        return hostname != null && PRIMARY_HOST.equals(hostname.trim().toLowerCase(Locale.ROOT));
    }

    static List<InetAddress> mergeDistinct(List<InetAddress> primary, List<InetAddress> secondary) {
        Map<String, InetAddress> ordered = new LinkedHashMap<>();
        addAll(ordered, primary);
        addAll(ordered, secondary);
        return new ArrayList<>(ordered.values());
    }

    static List<InetAddress> interleaveRecoveryAfterFirstSystem(
            List<InetAddress> system,
            List<InetAddress> recovery
    ) {
        if (system == null || system.isEmpty()) {
            return mergeDistinct(recovery, new ArrayList<>());
        }
        List<InetAddress> firstSystem = new ArrayList<>();
        firstSystem.add(system.get(0));
        List<InetAddress> preferred = mergeDistinct(firstSystem, recovery);
        return mergeDistinct(preferred, system);
    }

    private static void addAll(Map<String, InetAddress> output, List<InetAddress> addresses) {
        if (addresses == null) {
            return;
        }
        for (InetAddress address : addresses) {
            if (address != null) {
                output.put(address.getHostAddress(), address);
            }
        }
    }

    private static int distinctSize(List<InetAddress> addresses) {
        return mergeDistinct(addresses, new ArrayList<>()).size();
    }

    private static final class CacheEntry {
        final long loadedAtMs;
        final List<InetAddress> addresses;

        CacheEntry(long loadedAtMs, List<InetAddress> addresses) {
            this.loadedAtMs = loadedAtMs;
            this.addresses = new ArrayList<>(addresses);
        }
    }
}
