package com.drbep.tvplayer;

final class ProgramArtworkResolver {
    private ProgramArtworkResolver() {
    }

    static String resolve(EpgRepository.EpgProgram program, ChannelItem channel) {
        return resolve(
                program == null ? "" : program.icon,
                channel == null ? "" : channel.logoUrl
        );
    }

    static String resolve(String programIconUrl, String channelLogoUrl) {
        String programIcon = cleanImageUrl(programIconUrl);
        if (!programIcon.isEmpty()) {
            return programIcon;
        }
        return cleanImageUrl(channelLogoUrl);
    }

    private static String cleanImageUrl(String url) {
        String clean = url == null ? "" : url.trim();
        if (clean.isEmpty()) {
            return "";
        }
        String lower = clean.toLowerCase();
        if ("null".equals(lower)
                || "undefined".equals(lower)
                || "none".equals(lower)
                || "about:blank".equals(lower)
                || lower.startsWith("data:")) {
            return "";
        }
        return clean;
    }
}
