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
        String programIcon = programIconUrl == null ? "" : programIconUrl.trim();
        if (!programIcon.isEmpty()) {
            return programIcon;
        }
        return channelLogoUrl == null ? "" : channelLogoUrl.trim();
    }
}
