package com.drbep.tvplayer;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Use the original Widevine manifest and the device-generated license challenge. */
final class SkyNativePlayback {
    static final class Route {
        final String manifest, license;
        Route(String manifest, String license) { this.manifest=manifest; this.license=license; }
    }
    static Route resolve(String value, String backend) { return resolve(value, backend, false); }
    static Route resolve(String value, String backend, boolean nativeUhd) {
        try {
            URI url=new URI(value), base=new URI(backend);
            if (!"https".equalsIgnoreCase(url.getScheme()) || url.getRawUserInfo()!=null
                    || url.getRawFragment()!=null || base.getRawAuthority()==null
                    || !url.getRawAuthority().equalsIgnoreCase(base.getRawAuthority())) return null;
            String prefix="/api/vod/private/skyshowtime/play/";
            String path=url.getRawPath();
            if (!path.startsWith(prefix)) return null;
            String id=path.substring(prefix.length());
            if (!id.matches("[A-Za-z0-9_-]{1,64}")) return null;
            List<String> query=new ArrayList<>();
            query.add(nativeUhd ? "profile=uhd" : "profile=hd");
            if(url.getRawQuery()!=null) for(String part:url.getRawQuery().split("&")) {
                String key=part.split("=",2)[0];
                if(!key.equals("profile") && !key.equals("drm") && !part.isEmpty()) query.add(part);
            }
            String origin=url.getScheme()+"://"+url.getRawAuthority();
            String suffix="?"+String.join("&",query);
            return new Route(origin+path+suffix, origin+"/api/vod/private/skyshowtime/license/"+id+suffix);
        } catch(Exception invalid) { return null; }
    }
}
