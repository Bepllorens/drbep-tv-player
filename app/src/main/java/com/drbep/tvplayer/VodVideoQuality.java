package com.drbep.tvplayer;

import androidx.media3.common.Format;
import java.util.Locale;

/** Common VOD labels: actual encoded dimensions, not invented vertical tiers. */
final class VodVideoQuality {
    static String key(Format format) {
        return format.width+"/"+format.height+"/"+format.bitrate+"/"+format.frameRate+"/"
                +format.sampleMimeType+"/"+format.codecs+"/"+PlaybackFormatBadges.video(format);
    }
    static String label(Format format) {
        String size=format.width>0&&format.height>0?format.width+" × "+format.height:"Resolución no informada";
        if(format.width>=3840)size="4K · "+size;
        String hdr=PlaybackFormatBadges.video(format);
        String codec=PlaybackDiagnosticsFormatter.compactCodec(format.codecs==null?format.sampleMimeType:format.codecs);
        return size+" · "+(hdr.isEmpty()?"SDR":hdr)+" · "+codec
                +(format.bitrate>0?String.format(Locale.getDefault()," · %.1f Mb/s",format.bitrate/1_000_000d):"");
    }
}
