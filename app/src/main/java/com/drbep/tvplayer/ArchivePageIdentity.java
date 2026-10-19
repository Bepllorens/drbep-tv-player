package com.drbep.tvplayer;


/** Identifies the visible page, independently of background refresh status. */
final class ArchivePageIdentity {
    static String key(String day,String channel,String genre,String query,boolean saved,int offset) {
        StringBuilder key=new StringBuilder();
        for(String part:new String[]{day,channel,genre,query,Boolean.toString(saved),Integer.toString(offset)})
            key.append(part.length()).append(':').append(part);
        return key.toString();
    }
}
