package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Presentation only: representatives keep their original group/track coordinates. */
final class TrackOptionGrouping {
    static final class Candidate {
        final String identity;
        final boolean supported, selected;
        final int bitrate;
        Candidate(String identity, boolean supported, boolean selected, int bitrate) {
            this.identity=identity; this.supported=supported; this.selected=selected; this.bitrate=bitrate;
        }
    }
    static String identity(String language, String label, String mime, String codecs,
                           int channels, int sampleRate, int roles, int flags, int accessibilityChannel,
                           String unknownIdentity) {
        // Unlabelled tracks can be different commentaries; do not guess equivalence.
        if (clean(language).isEmpty() && clean(label).isEmpty()) return "unknown:" + unknownIdentity;
        return java.util.Arrays.asList(clean(language).replace('_','-'), clean(label), clean(mime), clean(codecs),
                channels, sampleRate, roles, flags, accessibilityChannel).toString();
    }
    static List<Integer> representatives(List<Candidate> candidates) {
        LinkedHashMap<String,Integer> chosen=new LinkedHashMap<>();
        for(int i=0;i<candidates.size();i++) {
            Candidate c=candidates.get(i);
            Integer previous=chosen.get(c.identity);
            if(previous==null || prefer(c,candidates.get(previous))) chosen.put(c.identity,i);
        }
        return new ArrayList<>(chosen.values());
    }
    private static boolean prefer(Candidate a,Candidate b) {
        if(a.supported!=b.supported)return a.supported;
        if(a.selected!=b.selected)return a.selected;
        return a.bitrate>b.bitrate;
    }
    private static String clean(String s){return s==null?"":s.trim().toLowerCase(Locale.ROOT);}
}
