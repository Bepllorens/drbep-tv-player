package com.drbep.tvplayer;

public final class U7dCatalogEntry {
    public ProgrammeMetadataUiModel programmeMetadata;
    public final String id,channelId,channel,title,genre,description,day;
    public final long start,end,expires;
    public boolean saved;
    public boolean hasProgramArtwork;
    public String artworkKey="";
    public String subtitle="";
    public String schedule="";
    public final java.util.function.Consumer<android.widget.ImageView> artwork;
    public final Runnable play,live,save;
    public U7dCatalogEntry(String id,String channelId,String channel,String title,String genre,String description,
            String day,long start,long end,boolean saved,java.util.function.Consumer<android.widget.ImageView> artwork,Runnable play,Runnable live,Runnable save) {
        this.id=id;this.channelId=channelId;this.channel=channel;this.title=title;this.genre=genre;this.description=description;
        this.day=day;this.start=start;this.end=end;this.expires=end+7L*86400000;this.saved=saved;this.artwork=artwork;this.play=play;this.live=live;this.save=save;
    }
}
