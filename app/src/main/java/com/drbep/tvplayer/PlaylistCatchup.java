package com.drbep.tvplayer;
import java.net.URI;import java.text.SimpleDateFormat;import java.util.*;
import org.json.JSONObject;
final class PlaylistCatchup {
    static String url(String live,JSONObject attributes,long start,long end,long now)throws Exception {
        int days=attributes.optInt("catchup-days",0);String template=attributes.optString("catchup-source"),type=attributes.optString("catchup",attributes.optString("catchup-type"));
        if(days<=0||days>365||start>=now||end<=start||start<now-days*86400000L||template.isEmpty())throw new IllegalArgumentException("Programme outside catch-up window");
        if(!type.isEmpty()&&!Arrays.asList("default","append","shift").contains(type))throw new IllegalArgumentException("Unsupported catchup mode");
        String value=template;Map<String,String> tokens=new LinkedHashMap<>();
        for(String k:Arrays.asList("utc","start","timestamp","start-timestamp"))tokens.put(k,Long.toString(start/1000));
        for(String k:Arrays.asList("end","utcend"))tokens.put(k,Long.toString(end/1000));
        for(String k:Arrays.asList("lutc","now"))tokens.put(k,Long.toString(now/1000));
        tokens.put("duration",Long.toString((end-start)/1000));tokens.put("offset",Long.toString((now-start)/1000));
        String[][] parts={{"Y","yyyy"},{"m","MM"},{"d","dd"},{"H","HH"},{"M","mm"},{"S","ss"}};
        for(String[] p:parts){SimpleDateFormat f=new SimpleDateFormat(p[1],Locale.ROOT);f.setTimeZone(TimeZone.getTimeZone("UTC"));tokens.put(p[0],f.format(new Date(start)));}
        for(Map.Entry<String,String> t:tokens.entrySet())value=value.replace("${"+t.getKey()+"}",t.getValue()).replace("{"+t.getKey()+"}",t.getValue());
        if(value.contains("{")||value.contains("}"))throw new IllegalArgumentException("Unknown catchup placeholder");
        if(type.equals("append"))value=live+value;
        URI u=new URI(value);if(u.getHost()==null||!(u.getScheme().equals("http")||u.getScheme().equals("https")))throw new IllegalArgumentException("Invalid catchup URL");return value;
    }
}
