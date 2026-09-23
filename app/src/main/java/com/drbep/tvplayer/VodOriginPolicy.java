package com.drbep.tvplayer;

/** Visibility is based on entitlements, never item count or update channel. */
final class VodOriginPolicy {
    static boolean allowed(OfflinePermissions permissions, String origin) {
        if (permissions == null || !permissions.vodEnabled) return false;
        switch (origin) {
            case "ALL": return permissions.hasVodCatalogAccess();
            case "HBO": return permissions.allowsHboVod();
            case "SKYSHOWTIME": return permissions.allowsSkyVod();
            case "MOVISTAR": return permissions.allowsMovistarVod();
            case "TIVIFY": return permissions.allowsTivifyVod() || permissions.allowsTivifyAdultVod();
            case "RUNTIME": return permissions.allowsRuntimeVod();
            case "PLEX": return permissions.allowsPlexVod();
            case "PRIME": return permissions.allowsPrimeVod();
            case "DAZN": return permissions.allowsDaznVod();
            case "DISNEYPLUS": return permissions.allowsDisneyplusVod();
            case "APPLETV": return permissions.allowsAppleTVVod();
            case "NETFLIX": return permissions.allowsNetflixVod();
            default: return false;
        }
    }
}
